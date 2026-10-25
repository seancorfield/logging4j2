;; copyright (c) 2024-2026 Sean Corfield

(ns build
  (:require [babashka.deps-deploy :as dd]
            [babashka.tasks :refer [shell]]
            [clojure.tools.build.api :as b]
            [clojure.tools.deps.edn :as deps]))

(def lib 'com.github.seancorfield/logging4j2)
(def version (-> (deps/project-deps) :aliases :neil :project :version))
(def snapshot
  (let [[_ m-m rel] (re-find #"(.*)\.(\d+)$" version)]
    (str m-m "." (inc (parse-long rel)) "-SNAPSHOT")))
(def class-dir "target/classes")

(defn- pom-template [version]
  [[:description "A log4j2 alternative to clojure.tools.logging."]
   [:url "https://github.com/seancorfield/logging4j2"]
   [:licenses
    [:license
     [:name "Eclipse Public License 2.0"]
     [:url "https://www.eclipse.org/legal/epl-2.0"]]]
   [:developers
    [:developer
     [:name "Sean Corfield"]]]
   [:scm
    [:url "https://github.com/seancorfield/logging4j2"]
    [:connection "scm:git:https://github.com/seancorfield/logging4j2.git"]
    [:developerConnection "scm:git:ssh:git@github.com:corfield/logging4j2.git"]
    [:tag (str "v" version)]]])

(defn- jar-opts [opts]
  (let [version (if (:snapshot opts) snapshot version)]
    (println "\nVersion:" version)
    (assoc opts
           :lib lib   :version version
           :jar-file  (format "target/%s-%s.jar" lib version)
           :basis     (b/create-basis {})
           :class-dir class-dir
           :target    "target"
           :src-dirs  ["src"]
           :pom-data  (pom-template version))))

(defn jar "Build the JAR."
  {:org.babashka/cli {:spec {:snapshot {:coerce :boolean}}}}
  [opts]
  (let [opts (jar-opts opts)]
    (b/delete {:path "target"})
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println "\nBuilding" (:jar-file opts) "...")
    (b/jar opts))
  opts)

(defn deploy "Deploy the JAR to Clojars."
  {:org.babashka/cli {:spec {:snapshot {:coerce :boolean}}}}
  [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (dd/deploy {:installer :remote :artifact (b/resolve-path jar-file)
                :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}))
  opts)

;; test-related utilities and tasks:

(defn- get-versions [opts]
  (if (:all-versions opts) ["1.11" "1.12" "1.13"] ["1.11"]))

(defn run-tests "Run tests for the specified Clojure versions."
  {:org.babashka/cli {:spec {:all-versions {:coerce :boolean}}}}
  [opts]
  (let [versions (get-versions opts)]
    (doseq [v versions]
      (println "\nTesting Clojure" v)
      (shell {:extra-env {"LOG4J_CONFIGURATION_FILE" "log4j2-test.properties"}}
             (str "clojure -M"
                  ":" v
                  ":test:runner")))))
