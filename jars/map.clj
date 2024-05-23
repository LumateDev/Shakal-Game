
(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))

(def map-size 30) ;задаём размер карты
(def player-symbol "\u001b[46m\u001b[36;1mX\u001b[0m")

(def map-cell "\u001b[43m\u001b[33m.\u001b[0m") ;задаём символ клетки карты
(def border-cell "\u001b[44m\u001b[34m#\u001b[0m") ;задаём границы карты
(def treasure-symbol "$") ;задаём символ обозначающий сокровище

(def yell-bg "\033[43m") ; жёлтый цвет фона
(def grey-background "\u001b[40m") ; Серый цвет фона

(def yellow "\u001b[33m") ; жёлтый цвет текста
(def green "\u001b[32m") ; зелёный цвет текста
(def red "\u001b[31m") ; красный цвет текста

(def nrm-bg  "\033[0m")  ; вернуть норм цвет


(defn colorize [color-code text]
  (str color-code text nrm-bg)) ; функция смены цвета


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
            (let [cell (if (and (= x player-x) (= y player-y)) player-symbol (get-in game-map [y x]))] ;хз, почему нужно менять меставми координаты, но иначе дублируется игрок
                (print cell)))
        (println-win "")))

(defn get-random-empty-cell [game-map] ;получение случайной клетки на карте
    (let [x (+ 1 (rand-int (inc (- (- map-size 2) 1)))) y (+ 1 (rand-int (inc (- (- map-size 2) 1))))] [x y]))


(defn place-treasures [game-map treasure-count treasure-symbol]
  (loop [current-map game-map, treasures-left treasure-count]
    (if (zero? treasures-left)
      current-map
      (let [[x y] (get-random-empty-cell current-map)]
        (if (= (get-in current-map [y x]) map-cell)
          (recur (assoc-in current-map [y x] treasure-symbol) (dec treasures-left))
          (recur current-map treasures-left))))))


(defn get-static-empty-cell [game-map] [0 0]) ;получения статичной клетки на карте

(defn place-player [game-map x y] ;функция, которая помещает игрока на переданную клетку
    (println-win (str "Placing player at coordinates [" x "," y "]")) ;для отладки
    (assoc-in game-map [y x] player-symbol))

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


(defn print-lives [player-lives]
  (println-win (colorize grey-background (str "HP: " (colorize red (apply str (take player-lives (repeat  "<3 ")))))))) ; функция отображения жизней

(defn decrease-lives [player-lives]
  (dec player-lives))  ; функция отнимания жизней (пока нигде не вызывается, после может вызыватся в сражениях, или при наступании на ловушки)

(defn money-message [money]
    (println-win (colorize yellow (str "Current balance has been increased to: " money "\n"))))
    
; Функция для постоянного отображения текущего баланса
(defn print-balance [player-balance]
  (if (= player-balance 0)
    (println-win (colorize grey-background (str "Balance: " (colorize green "0"))))
    (println-win (colorize grey-background (str "Balance: " (colorize green (apply str (take player-balance (repeat "$")))))))))

(defn move-player [game-map explored x y dx dy player-balance treasure-symbol] ;перемещение игрока
    (let [new-x (+ x dx) new-y (+ y dy) map-size (count game-map) border-cell "\u001b[44m\u001b[34m#\u001b[0m"]
        (if (or (< new-x 0) (> new-x (dec map-size)) ;проверяем новый х
                (< new-y 0) (> new-y (dec map-size)) ;проверяем новый y
                (= (get-in game-map [new-y new-x]) border-cell)) ;проверяем, является ли новая позиция границей
            (do (println-win (colorize red "You cannot move outside the island until you collect all the treasures \u001b[47m\u001b[32m$\u001b[0m \u001b[31mor defeat all the enemies \u001b[41m\u001b[30;1mX\u001b[0m\u001b[31m! \u001b[0m\n")) ;показываем предупреждение
                [game-map explored player-balance]) ;возвращаем неизменную карту и структуру открытых клеток
            (let [current-cell (get-in game-map [new-y new-x])]
                (if (= current-cell treasure-symbol)
                    (do (money-message (str (inc player-balance))) ;уведомляем о пополнении счёта (перевожу ещё и в строку так как нужно для смены цвета)
                        [(assoc-in (assoc-in game-map [y x] map-cell) [new-y new-x] player-symbol) ;обновляем карту
                         (update-explored explored new-x new-y 1) ;обновляем исследованные клетки
                         (inc player-balance)]) ;увеличиваем баланс
                    (try
                        [(assoc-in (assoc-in game-map [y x] map-cell) [new-y new-x] player-symbol) ;пытаемся переместить персонажа
                         (update-explored explored new-x new-y 1) ;обновляем исследованные клетки
                         player-balance] ;баланс остается прежним
                        (catch IndexOutOfBoundsException e
                            (do (println-win "Caught IndexOutOfBoundsException. Ignoring movement (patchami popravim, chestno).") ;обрабатываем исключение
                                [game-map explored player-balance]))))))))

(defn print-map-with-explored [game-map explored] ;печать карты со структурой
    (doseq [y (range (count game-map))]
        (doseq [x (range (count (game-map y)))]
            (print (if (get-in explored [y x])
                (get-in game-map [y x])
                    " ")))
        (println-win "")))


