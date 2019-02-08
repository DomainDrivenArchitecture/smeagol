(ns smeagol.sample)

(defn pow [x]
  (int (Math/pow x 2)))

(defn sum-values [x]
  (->> x vals (reduce +)))
