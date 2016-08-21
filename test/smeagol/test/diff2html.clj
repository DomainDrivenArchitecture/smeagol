(ns smeagol.test.diff2html
  (:use clojure.test
        ring.mock.request
        smeagol.diff2html))

(deftest test-mung-line
  (testing "conversion of individual lines"
    (is
      (= (mung-line "+This is a test") "<p><ins>This is a test</ins></p>")
      "Insertions should be marked as such")
    (is
      (= (mung-line "-This is a test") "<p><del>This is a test</del></p>")
      "Insertions should be marked as such")
    (is
      (= (mung-line "\\This is a test") "<p class='warn'>This is a test</p>")
      "Lines starting with a backslash are suspect")
    (is
      (= (mung-line "") "<p></p>") "Blank lines should become empty paragraphs")))

