(ns deps
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [tangle.core :as tangle]))

(defn clean-path [p]
  (string/replace p "$PROJECT_DIR$" ""))

(defn module-from-path [p]
  (let [[_ _ mod] (string/split p #"/")]
    (str "/x/" mod "/")))

(defn test-file? [p]
  (or (string/ends-with? p "_test.go")
      (string/includes? p "testutil")))

(defn module-file-edges [mode f module]
  (->> (xml/parse f)
       (:content)
       (mapcat (fn [file-node]
                 (let [path (-> file-node :attrs :path clean-path)]
                   (map (fn [dep-node]
                          {:file path
                           :dep (-> dep-node :attrs :path clean-path)})
                        (:content file-node)))))
       (filter (fn [{:keys [file dep]}]
                 (and (string/includes? file module)
                      (not (string/includes? dep module)))))
       (filter (fn [{:keys [file]}]
                 (condp = mode
                   :all true
                   :prod (not (test-file? file))
                   :test (test-file? file))))
       (keep (fn [{:keys [file dep]}]
               (when (string/starts-with? dep "/x/")
                 [file dep])))))

(defn modules [f]
  (->> (xml/parse f)
       (:content)
       (map (comp module-from-path clean-path :path :attrs))
       (distinct)
       (remove #{"/x/README.md"})))

(def dependency-xml "./x-deps.xml")

(defn graph-from-edges [edges]
  (reduce (fn [g [m dependency]]
            (update g m (fnil conj #{}) dependency))
          {}
          edges))

(defn edges-from-graph [g]
  (mapcat (fn [[n es]]
            (map (partial vector n) es))
          g))

(defn dep-edges [mode]
  (->> (modules dependency-xml)
       (mapcat (partial module-file-edges mode dependency-xml))
       (map (partial map module-from-path))
       (set)))

(def dep-graph (comp graph-from-edges dep-edges))

(defn print-edges [edges]
  (doseq [[a b] edges]
    (println a "->" b)))

(defn print-files [mode]
  (->> (modules dependency-xml)
       (mapcat (partial module-file-edges mode dependency-xml))
       (print-edges)))

;; Question:
;; What novel dependencies are introduced in the test phase only?
;;  - List these per module, per file.

(defn test-only-edges
  []
  (set/difference (dep-edges :all) (dep-edges :prod)))

(defn novel-test-refs
  []
  (let [test-only? (set (test-only-edges))]
    (->> (modules dependency-xml)
         (mapcat (partial module-file-edges :test dependency-xml))
         (filter (fn [[a b]]
                   (test-only? [(module-from-path a) (module-from-path b)]))))))

(defn visualize [edges]
  (-> (tangle/graph->dot
       (set (map first edges))
       edges
       {:directed? true}
       )
      (tangle/dot->image "png")
      (io/copy (io/file "./out.png"))))
