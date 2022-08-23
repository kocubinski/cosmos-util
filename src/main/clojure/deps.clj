(ns deps
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.string :as string]))

(defn clean-path [p]
  (string/replace p "$PROJECT_DIR$" ""))

;; example usage: (print-report (parse-report "./auth-deps.xml" "x/" "x/auth"))
;;
(defn module-report [f module]
  (->> (xml/parse f)
       (:content)
       (mapcat (fn [file-node]
                 (let [path (-> file-node :attrs :path clean-path)]
                   (map (fn [dep-node]
                          {:file path
                           :dep (-> dep-node :attrs :path clean-path)})
                        (:content file-node)))
                  ))
       (filter (fn [{:keys [file dep]}]
                 (and (string/includes? file module)
                      (not (string/includes? dep module)))))
       (keep (fn [{:keys [file dep]}]
               (when (string/starts-with? dep "/x/")
                 [file dep])))
       )
  )

(defn modules [f]
  (->> (xml/parse f)
       (:content)
       (map (comp clean-path :path :attrs))
       (map (fn [p]
              (let [[_ _ mod] (string/split p #"/")]
                (str "/x/" mod))))
       (distinct)
       (remove #{"/x/README.md"})))

(defn print-report []
  (let [f "./x-deps.xml"]
    (doseq [m (modules f)]
      (doseq [[file dep] (module-report f m)]
        (println file "->" dep)))))
