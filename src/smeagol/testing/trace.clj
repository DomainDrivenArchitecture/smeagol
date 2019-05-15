(ns smeagol.testing.trace
  (:require [clojure.tools.trace :as trace]
            [clojure.test]
            [clojure.pprint :refer [pprint]]
            [clojure.java.io :as io]
            [smeagol.configuration :refer [config]])
  (:import [java.io FileNotFoundException]))

;; clojure.test:
;; { FILENAME { HEADING { fn-name {}}}}
;; {test-ns-sym {test-ns-var-sym { fn-name {:in :out}})}}}
(def calls (atom {}))

(defn current-clojure-test []
  (when-let [[v & _] clojure.test/*testing-vars*]
    (let [test-ns-sym (-> v meta :ns ns-name)
          test-ns-var-sym (-> v meta :name)]
      [test-ns-sym test-ns-var-sym])))

(defn current-test [fn-name]
  (let [ns-sym (symbol (namespace fn-name))
        ns-var-sym (symbol (name fn-name))]
    [ns-sym ns-var-sym]))

(defn tracer
  [name {:keys [fn-name] :as value-map}]
  (let [atom-path (or (current-clojure-test) (current-test fn-name))
        desc (clojure.test/testing-contexts-str)]
    (swap! calls update-in atom-path #(with-meta (assoc % fn-name value-map)
                                        {:desc desc}))))

(defn trace-fn-call
  [name f args]
  (println "calling trace-fn-call")
  (let [id (gensym "t")]
    #_(if (= 0 (var-get #'trace/*trace-depth*))
      (tracer id {:fn-name name :in (vec args)}))
    (let [value (binding [trace/*trace-depth* (inc (var-get #'trace/*trace-depth*))]
                  (apply f args))]
      (if (= 0 (var-get #'trace/*trace-depth*))
        (tracer id {:fn-name name :out value :in (vec args)}))
      value)))

(defn safe-ns-require [ns-sym]
  (try
    (require ns-sym)
    ns-sym
    (catch FileNotFoundException e nil)))

(defn traced-test-nses [config]
  (when-let [ns-names (-> config :test-namespaces keys)]
    (->> ns-names
         (map (comp safe-ns-require symbol))
         (filter identity))))

(defn clear-ns-calls [ns-sym]
  (swap! calls dissoc ns-sym))

(defn md-file [config ns-sym var-sym]
  (let [f (io/file (:content-dir config) "smeagol" (name ns-sym) (str (name var-sym) ".md"))]
    (println {:f f})
    (io/make-parents f)
    (io/delete-file f true)
    f))

(defn render-md [{:keys [fn-name] :as test}]
  (str "```test " fn-name "\n"
       (with-out-str (pprint (select-keys test [:in :out])))
       "```"))

(defn render-test [var-sym tests]
  (apply str
         (flatten
          (list "\n# " var-sym "\n"
                (:desc (meta tests)) "\n\n"
                (mapv (comp render-md val) tests)))))

(defn render-calls! [config calls]
  (doseq [[ns-sym tests] calls]
    (doseq [[var-sym tests] tests
          :let [f (md-file config ns-sym var-sym)
                content (render-test var-sym tests)]]
      (when content
        (println (pr-str {:into f
                          :content content}))
        (spit f content :append true)))))

(defmacro tracing
  "Should wrap call to test runner like
  (tracing (clojure.test/run-all-tests))"
  [& body]
  `(with-redefs [clojure.tools.trace/trace-fn-call trace-fn-call]
     (let [nses# (traced-test-nses config)]
       (run! (juxt trace/trace-ns* clear-ns-calls) nses#)
       (let [return# (do ~@body)]
         (run! trace/untrace-ns* nses#)
         (render-calls! config @calls)
         return#))))

