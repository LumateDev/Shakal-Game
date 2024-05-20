
(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))

(def map-size 15) ;задаём размер карты
(def map-cell ".") ;задаём символ клетки карты
(def border-cell "#") ;задаём границы карты

(defn create-empty-map [] ;создаём пустую карту
    (let [empty-row (vec (for [_ (range map-size)] map-cell))
          border-row (vec (for [_ (range map-size)] border-cell))]
        (vec (for [i (range map-size)]
            (if (or (= i 0) (= i (dec map-size)))
                border-row
                (vec (for [j (range map-size)]
                    (if (or (= j 0) (= j (dec map-size)))
                        border-cell
                        map-cell))))))))

(defn print-map [game-map] ;выводим карту в консоль
    (doseq [row game-map] (println-win (apply str row))))

(defn print-map-with-player [game-map player-x player-y] ;выводим карту с игроком в консоль
    (doseq [y (range (count game-map))]
        (doseq [x (range (count (first game-map)))]
            (let [cell (if (and (= x player-x) (= y player-y)) \X (get-in game-map [y x]))] ;хз, почему нужно менять меставми координаты, но иначе дублируется игрок
                (print cell)))
        (println-win "")))

(defn get-random-empty-cell [game-map] ;получение случайной клетки на карте
    (let [x (+ 1 (rand-int (inc (- 13 1)))) y (+ 1 (rand-int (inc (- 13 1))))] [x y]))

(defn get-static-empty-cell [game-map] [0 0]) ;получения статичной клетки на карте

(defn place-player [game-map x y] ;функция, которая помещает игрока на переданную клетку
    (println-win (str "Placing player at coordinates [" x "," y "]")) ;для отладки
    (assoc-in game-map [y x] \X))

(defn initialize-explored [map-size] ;созаём вспомогательную структуру
    (vec (for [_ (range map-size)] (vec (for [_ (range map-size)] false)))))

(defn update-explored [explored x y radius] ;дополнительная структура исследования карты
  (reduce (fn [exp [i j]]
            (if (and (>= i 0) (< i map-size)
                     (>= j 0) (< j map-size))
              (assoc-in exp [j i] true)
              exp))
          explored
          (for [dx (range (- radius) (inc radius))
                dy (range (- radius) (inc radius))]
            [(+ x dx) (+ y dy)])))

(defn move-player [game-map explored x y dx dy] ;перемещение игрока
    (let [new-x (+ x dx) new-y (+ y dy) map-size (count game-map) border-cell "#"]
    (if (or (< new-x 0) (> new-x (dec map-size)) ;проверяем новый х
            (< new-y 0) (> new-y (dec map-size)) ;проверяем новый y
            (= (get-in game-map [new-y new-x]) border-cell)) ;проверяем, является ли новая позиция границей
        (do (println-win "You can't move there! It's a boundary or outside the map.") ;показываем предупреждение
            [game-map explored]) ;возвращаем неизменную карту и структуру открытых клеток
        (try
            [(assoc-in (assoc-in game-map [y x] \.) [new-y new-x] \X) ;пытаемся переместить персонажа
                (update-explored explored new-x new-y 1)]
            (catch IndexOutOfBoundsException e
                (do (println-win "Caught IndexOutOfBoundsException. Ignoring movement (patchami popravim, chestno).") ;обрабатываем исключение
                    [game-map explored])))))) ;возвращаем неизменную карту и структуру открытых клеток

(defn print-map-with-explored [game-map explored] ;печать карты со структурой
    (doseq [y (range (count game-map))]
        (doseq [x (range (count (game-map y)))]
            (print (if (get-in explored [y x])
                (get-in game-map [y x])
                    " ")))
        (println-win "")))
