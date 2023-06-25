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
       ;; uncomment to exclude pb generated files for RFC research
       ;; (filter (fn [{:keys [dep]}]
       ;;           (string/includes? dep ".pb.")))
       ;;
       ;; only include .go files
       (filter (fn [{:keys [dep]}]
                 (string/ends-with? dep ".go")))
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

(defn graph-from-edges [edges]
  (reduce (fn [g [m dependency]]
            (update g m (fnil conj #{}) dependency))
          {}
          edges))

(defn edges-from-graph [g]
  (mapcat (fn [[n es]]
            (map (partial vector n) es))
          g))

(defn make-dep-edges
  "Example usage: (make-dep-edges :prod './x-deps-rm-getsignbytes')"
  [mode xml-file]
  (->> (modules xml-file)
       (mapcat (partial module-file-edges mode xml-file))
       (map (partial map module-from-path))
       (set)))

(defn print-edges [edges]
  (doseq [[a b] edges]
    (println a "->" b)))

(defn print-files [mode xml-file]
  (->> (modules xml-file)
       (mapcat (partial module-file-edges mode xml-file))
       (print-edges)))

;; Question:
;; What novel dependencies are introduced in the test phase only?
;;  - List these per module, per file.

(defn test-only-edges
  [xml-file]
  (set/difference (make-dep-edges xml-file :all) (make-dep-edges xml-file :prod)))

(defn novel-test-refs
  [xml-file]
  (let [test-only? (set (test-only-edges))]
    (->> (modules xml-file)
         (mapcat (partial module-file-edges :test xml-file))
         (filter (fn [[a b]]
                   (test-only? [(module-from-path a) (module-from-path b)]))))))

(defn cyclic-dependencies
  [g]
  (->> g
       (reduce (fn [cd [m deps]]
                 (->> deps
                      (filter (fn [dep]
                            ;; for each dep of m, where dep also depends on m ))
                                (when-let [g-deps (g dep)]
                                  ((g dep) m))))
                      (map (comp sort (partial vector m)))
                      (concat cd)))
               (list))
       (distinct)))

(defn print-cyclic [edges]
  (doseq [s (->> (sort-by first edges)
                 (map (fn [[a b]] (str a " <-> " b)))
                 (distinct))]
    (println s)))

(defn visualize [edges]
  (-> (tangle/graph->dot
       (set (map first edges))
       edges
       {:directed? true}
       )
      (tangle/dot->image "png")
      (io/copy (io/file "./out.png"))))


(comment
  (def main-xml "./x-deps-main-20230619.xml")
  (def branch-xml "./x-deps-auth-deps.xml")
  ;;
  ;; visualize the production dependency graph
  (visualize (make-dep-edges :prod main-xml))
  (visualize (make-dep-edges :prod branch-xml))
  ;;
  ;; visualize the dependency graph with tests included
  (visualize (make-dep-edges :all main-xml))
  (visualize (make-dep-edges :all branch-xml))
  ;;
  ;; print cyclic dependencies
  ;;
  (-> (make-dep-edges :all main-xml) graph-from-edges cyclic-dependencies print-cyclic)
  (-> (make-dep-edges :prod branch-xml) graph-from-edges cyclic-dependencies print-cyclic)
  )
