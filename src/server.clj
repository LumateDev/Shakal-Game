#!/usr/bin/env clj

(ns server ;простарнство имён server будет использовать пространства имён clojure.contrib.server-socket и clojure.contrib.duck-streams
    (:use [clojure.contrib server-socket duck-streams]) ;server-socket работает с сокетами, а duck-streams работает с потоками ввода-вывода
    (:require [welcome :refer [welcome-in-game]]) ;импортируем функцию-приветствие
    (:require [map :refer [create-empty-map print-map get-random-empty-cell get-static-empty-cell place-player
                           initialize-explored update-explored move-player print-map-with-explored place-treasures place-players start-game]]) ;импортируем функции для работы с картой
    ; (:import [java.util Timer TimerTask])
)

;здесь объявляются все константы мультиплеера
(def connections (atom []))
(def port 5555)  ; будем подключаться к серверу по порту 5555
(def max-players 2) ; максимальное колличество игроков
(def players (atom [])) 
(def current-turn (atom nil)) ; текущий код
(def turn-queue (atom []))


(def exit-keyword "exit") ;кодовое слово для отключения с сервера
(def game-map (create-empty-map)) ;создаём пустую карту

(def user-name (atom nil))

(def treasure-symbol "\u001b[47m\u001b[32m$\u001b[0m") ;задаём символ обозначающий сокровище
(def treasure-count 10) ;задаём количество сокровищы

(def common-weapons-symbol "\u001b[47m\u001b[30;1m!\u001b[0m")
(def common-weapons-count 5)
(def rare-weapons-symbol "\u001b[42m\u001b[37;1m|\u001b[0m")
(def rare-weapons-count 3)
(def legendary-weapons-symbol "\u001b[45m\u001b[33;1m}\u001b[0m")
(def legendary-weapons-count 1)

(def common-armor-symbol "\u001b[47m\u001b[30;1m^\u001b[0m")
(def common-armor-count 5)
(def rare-armor-symbol "\u001b[42m\u001b[37;1m)\u001b[0m")
(def rare-armor-count 3)
(def legendary-armor-symbol "\u001b[45m\u001b[33;1m]\u001b[0m")
(def legendary-armor-count 1)

(def player-lives 3) ; начальное колличество жизней
(def player-armor 0) ; начальное колличество очков защиты
(def player-damage 1) ; начальное колличество очков урона
(def player-symbol "\u001b[46m\u001b[36;1mX\u001b[0m") ; это игрок
(def enemies-symbol "\u001b[41m\u001b[30;1mX\u001b[0m") ; это враги


(defn schedule-read-log-and-display []
  (let [executor (. java.util.concurrent.Executors newScheduledThreadPool 1)]
    (.scheduleAtFixedRate executor read-log-and-display 0 3 java.util.concurrent.TimeUnit/SECONDS)))

(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))


(defn recur-and-print-map ; Функция для рекурсивной смены состояния карты сервера, вызывает функцию непосредственной записи текущей карты в файл логов
        [game-map]
        (print-map-server game-map)
    )






;добавляем плеера к вектору всех игроков
(defn add-player-to-players [player]
  (swap! players conj player) ; 1. Добавление нового игрока в список игроков.
  ;(println-win (str "Current players: " @players)) ; 2. Вывод текущего списка игроков в консоль.
)

;ищем игрока по имени
(defn find-player-by-name [players name]
  (first (filter #(= (:name %) name) players)))




;добавляем в массив очереди имена
(defn add-player-to-queue [player]
  (swap! turn-queue conj (get player :name) ))

;зацикливаем очередь и делаем шаг вперед
(defn next-player []
  (let [current (first @turn-queue)
        remaining (rest @turn-queue)]
    (reset! current-turn current)
    (reset! turn-queue (conj (vec remaining) current))))

;проверка переменной текущий ход с именем игрока
(defn current-player-turn? [name]
   (=  @current-turn name))






;добавляем игрока сразу в очередь и в стек игроков
(defn add-player [player]
  (add-player-to-players player)
  (add-player-to-queue player)

)

(defn update-thread [] ; тормозок
    (read-line)
    (println-win "wait...")
)

;ожидает подключения всех игроков
(defn wait-for-players []
  (while (< (count @players) max-players)
    (println-win (str "Waiting for players...  " max-players))
    (update-thread) ; Вызываем тормозок (так как у меня не работаю потоки то вот таой вариант, если работают потоки, коммитте это и откоммитте следующую строку)
    ; (Thread/sleep 1000)
    )
  (println-win "All players are ready! The game is starting..."))


; ищет игрока по имени и обновляет его данные
(defn update-player-by-name [players-atom player-name update-fn]
  (swap! players-atom
         (fn [players]
           (map
             (fn [player]
               (if (= (:name player) player-name)
                 (update-fn player)
                 player))
             players))))



; частные случай обновления. обновляет только координаты
(defn update-player-coordinates [players-atom player-name new-x new-y]
  (update-player-by-name players-atom player-name
                         (fn [player]
                           (assoc player :player-x new-x :player-y new-y))))

 


(defn game-loop [game-map player output-stream]
  
  (let [{:keys [player-x player-y explored player-lives player-armor player-damage player-balance name]} player]
  (next-player)
  (loop [game-map game-map
         explored explored
         player-x player-x
         player-y player-y
         player-lives player-lives
         player-armor player-armor
         player-damage player-damage
         player-balance player-balance
         name name]
    
    (println-win (str "NICK: " name)) ; Имя пользхователя текущего потока

    (println-win (str "Current queue: " @turn-queue)) ; Очередь ходов


    
    (update-player-coordinates players name player-x player-y)  ;обновляем координаты игрока со значениями, которые пришли на прошлой итерации
    ; (print @players) 
    (println-win "\u001b[32;1mYour stats:\u001b[0m")
    (print-lives player-lives)
    (print-armor player-armor)
    (print-damage player-damage)
    (print "\n")
    (println-win "\u001b[33;1mYour inventory:\u001b[0m")
    (print-current-armor player-armor)
    (print-current-weapon player-damage)
    (print-balance player-balance)
    (print "\n")
    (print-map-with-explored game-map explored)
    (flush)
    (if (current-player-turn? name)(do ;если ход текущего игрока
    (let [input (read-line)]
      (cond
        (= input "exit")
        (do
          (println-win "\u001b[32mThank you for playing the Jackal Game!\u001b[0m\n")
          
          (flush)
          (.close output-stream))

        (= input "w")
        (let [[new-map new-explored new-balance new-armor new-damage]
              (move-player game-map explored player-x player-y 0 -1 player-armor player-damage player-balance treasure-symbol)]
          (if (= new-map game-map)
            (do
              (next-player) ; передает ход
              (recur-and-print-map new-map) ; КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance name))
            (do
              (next-player); передает ход
              (recur-and-print-map new-map); КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
              (recur new-map new-explored player-x (dec player-y) player-lives new-armor new-damage new-balance name))))

        (= input "a")
        (let [[new-map new-explored new-balance new-armor new-damage]
              (move-player game-map explored player-x player-y -1 0 player-armor player-damage player-balance treasure-symbol)]
          (if (= new-map game-map)
            (do
              (next-player); передает ход
              (recur-and-print-map new-map); КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
               (update-player-coordinates players name (- player-x 1) (+ player-y 0))
                              
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance name))
            (do
              (next-player); передает ход
              (recur-and-print-map new-map); КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
              (recur new-map new-explored (dec player-x) player-y player-lives new-armor new-damage new-balance name))))

        (= input "s")
        (let [[new-map new-explored new-balance new-armor new-damage]
              (move-player game-map explored player-x player-y 0 1 player-armor player-damage player-balance treasure-symbol)]
          (if (= new-map game-map)
            (do
              (next-player); передает ход
              (recur-and-print-map new-map); КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance name))
            (do
              (next-player); передает ход
              (recur-and-print-map new-map); КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
              (recur new-map new-explored player-x (inc player-y) player-lives new-armor new-damage new-balance name))))

        (= input "d")
        (let [[new-map new-explored new-balance new-armor new-damage]
              (move-player game-map explored player-x player-y 1 0 player-armor player-damage player-balance treasure-symbol)]
          (if (= new-map game-map)
            (do
              (next-player); передает ход
              (recur-and-print-map new-map); КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance name))
            (do
              (next-player); передает ход
              (recur-and-print-map new-map); КАЖДЫЙ РАЗ  ПЕРЕД РЕКУРСИВНЫМ ИЗМЕНЕНИЕМ КАРТЫ ДЛЯ ПОЛЬЗОВАТЕЛЯ 
              ; ПЕРЕДАЁМ ТЕКУЩЕЕ СОСТОЯНИЕ КАРТЫ В ФУНКЦИЮ ЗАПИСИ В ФАЙЛ
              (recur new-map new-explored (inc player-x) player-y player-lives new-armor new-damage new-balance name))))

        :else
        (do
          (println-win "\u001b[31mInvalid input. Use w, a, s, or d.\u001b[0m\n")
          (flush)
          
          (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance name)))))
 (do ;если не ход текущего игрока
        
        (println-win "Not your turn!")
        (update-thread) ; Вызываем тормозок (так как у меня не работаю потоки то вот таой вариант, если работают потоки, коммитте это и откоммитте следующую строку)
        ; (Thread/sleep 1000)
        (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance name))))))
;повторение всей красоты

(def game-map
  (let [initial-game-map (create-empty-map) ;создание пустой карты
        game-map-with-treasures (place-treasures initial-game-map treasure-count treasure-symbol) ;разместить сокровища на карте


        game-map-with-common-weapons (place-treasures game-map-with-treasures common-weapons-count common-weapons-symbol) ; разместить обычные оружия на карте
        game-map-with-rare-weapons (place-treasures game-map-with-common-weapons rare-weapons-count rare-weapons-symbol) ; разместить редкие оружия на карте
        game-map-with-legendary-weapons (place-treasures game-map-with-rare-weapons legendary-weapons-count legendary-weapons-symbol) ; разместить легендарные оружия на карте

        game-map-with-common-armor (place-treasures game-map-with-legendary-weapons common-armor-count common-armor-symbol) ; разместить обычныую броню на карте
        game-map-with-rare-armor (place-treasures game-map-with-common-armor rare-armor-count rare-armor-symbol) ; разместить редкую броню на карте
        game-map-with-legendary-armor (place-treasures game-map-with-rare-armor legendary-armor-count legendary-armor-symbol) ; разместить легендарную броню на карте  

        game-map-with-enemy (place-players game-map-with-legendary-armor @players enemies-symbol) ;должна создавать игроков, но не работает
    ]
    ;game-map-with-legendary-armor 
    game-map-with-enemy
  )
)


(defn server-base-fun [input-stream output-stream]
   (let [[player-x player-y] (get-random-empty-cell game-map) ;получаем клетку для игрока

         
         game-map (place-player game-map player-x player-y) ;спавним игрока
         explored (update-explored (initialize-explored (count game-map)) player-x player-y 1)  ;обновляем данные о карте
         player-balance 0]
        (println "\u001b[32m!Player initialized. Waiting for input...\u001b[0m") ;для отладки

        (print-map game-map) ; <--- выводим карту на сервере перед binding, иначе внутри binding до сервера не достучатся
        (schedule-read-log-and-display) ; ВЫЗЫВАЕМ ФУНКЦИЮ, КОТОРАЯ СОЗДАЁТ ОБЪЕКТ ПЛАНИРОВЩИКА ДЛЯ ВЫЗОВА ФУНКЦИИ СЧИТЫВАНИЯ ФАЙЛА КАЖДЫЕ 3 СЕКУНДЫ

        (binding [*in* (reader input-stream) *out* (writer output-stream)] ;биндим потоки ввода/вывода
        (welcome-in-game)
        (user-name-reader user-name)
        (add-player {:player-x player-x :player-y player-y :explored explored :player-lives player-lives :player-armor player-armor :player-damage player-damage :player-balance player-balance :name @user-name}) ;добавляем игрока
        (let [player (find-player-by-name @players @user-name)] player ; получаем текущего игрока
        (flush)
        (wait-for-players) ; ждем пока все игроки подключены
        (game-loop game-map player output-stream)))))



(defn handle-connection [input-stream output-stream]
   (swap! connections conj {:input-stream input-stream :output-stream output-stream }) ;добавляем новое соединение в список
  (server-base-fun input-stream output-stream))

(def server (create-server port handle-connection)) 