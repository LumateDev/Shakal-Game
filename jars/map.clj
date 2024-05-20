
(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))

(def map-size 15) ;задаём размер карты
(def map-cell ".") ;задаём символ клетки карты

(defn create-empty-map [] ;создаём пустую карту
    (let [empty-row (vec (for [_ (range map-size)] map-cell))]
        (vec (for [_ (range map-size)] empty-row))))

(defn print-map [game-map] ;выводим карту в консоль
    (doseq [row game-map] (println-win (apply str row))))

(defn print-map-with-player [game-map player-x player-y] ;выводим карту с игроком в консоль
    (doseq [y (range (count game-map))]
        (doseq [x (range (count (first game-map)))]
            (let [cell (if (and (= x player-x) (= y player-y)) \X (get-in game-map [y x]))] ;хз, почему нужно менять меставми координаты, но иначе дублируется игрок
                (print cell)))
        (println-win "")))

(defn get-random-empty-cell [game-map] ;получение случайной клетки на карте
    (let [x (rand-int map-size) y (rand-int map-size)] [x y]))

(defn get-static-empty-cell [game-map] [0 0]) ;получения статичной клетки на карте

(defn place-player [game-map x y] ;функция, которая помещает игрока на переданную клетку
    (println-win (str "Placing player at coordinates [" x "," y "]")) ;для отладки
    (assoc-in game-map [y x] \*))

(defn move-player [game-map x y dx dy] ;функция перемещения игрока
    (let [new-x (+ x dx) new-y (+ y dy) map-size (count game-map)]
        (if (or (< new-x 0) (> new-x (dec map-size)) ;проверяем новый х
                (< new-y 0) (> new-y (dec map-size))) ;проверяем новый y
            (do (println-win "You can't go outside the map!") ;показываем предупреждение
                game-map) ;возвращаем неизменную карту
            (try
                (assoc-in (assoc-in game-map [y x] \.) [new-y new-x] \X) ;пытаемся переместить персонажа
                (catch IndexOutOfBoundsException e
                (println-win "Caught IndexOutOfBoundsException. Ignoring movement (P.S. patchami popravim, chestno).") ;обрабатываем исключение
                game-map))))) ;возвращаем неизменную карту
