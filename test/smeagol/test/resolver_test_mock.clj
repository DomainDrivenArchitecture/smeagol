(ns smeagol.test.resolver-test-mock
  (:require [schema.core :as s]
            [integrant.core :as ig]
            [smeagol.include.resolve :as resolve]
            ))

(def component-id :include/test-mock)

(defmethod ig/init-key component-id [_]  
  (s/defmethod resolve/do-resolve-md component-id
    [_
     uri :- s/Str]
    (cond
      (= uri "./simple.md") "Simple content."
      (= uri "./with-heading-and-list.md") "# Heading2
some text
* List

## Heading 3
more text")))

(defmethod ig/halt-key! component-id [_ resolver]
  (remove-method resolve/do-resolve-md component-id))
