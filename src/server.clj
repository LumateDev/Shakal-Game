#!/usr/bin/env clj

(ns server ;простарнство имён server будет использовать пространства имён clojure.contrib.server-socket и clojure.contrib.duck-streams
    (:use [clojure.contrib server-socket duck-streams]) ;server-socket работает с сокетами, а duck-streams работает с потоками ввода-вывода
    (:require [welcome :refer [welcome-in-game]]) ;импортируем функцию-приветствие
    (:require [map :refer [create-empty-map print-map get-random-empty-cell get-static-empty-cell place-player
                           initialize-explored update-explored move-player print-map-with-explored place-treasures start-game]]) ;импортируем функции для работы с картой
    ; (:import [java.util Timer TimerTask])
)
(def connections (atom []))
(def port 5555) ;будем подключаться к серверу по порту 5555
(def max-layers 2)

(def game-state (atom { :status false }))



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



(def max-players 2)

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

(def players (atom [])) 

(defn add-player [player-info]
  (swap! players conj player-info) ; 1. Добавление нового игрока в список игроков.
  (println "Current players:" @players) ; 2. Вывод текущего списка игроков в консоль.
  (let [player-id (:id player-info)] ; 3. Определение идентификатора нового игрока.
    (swap! connections conj player-info)))
    

(defn find-player-by-name [players name]
  (first (filter #(= (:name %) name) players)))


()

;; (defn print-players [players]
;;   (println "Current players:" @players))


(defn game-loop [game-map explored player-x player-y player-lives player-armor player-damage player-balance output-stream]
  (loop [game-map game-map
         explored explored
         player-x player-x
         player-y player-y
         player-lives player-lives
         player-armor player-armor
         player-damage player-damage
         player-balance player-balance]
    (print "NICK: ")
    (print @connections)
    (println "Current players:" @players)
    (print-user-name @user-name)   
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
              (recur-and-print-map new-map)
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance ))
            (do
              (recur-and-print-map new-map)
              (recur new-map new-explored player-x (dec player-y) player-lives new-armor new-damage new-balance ))))

        (= input "a")
        (let [[new-map new-explored new-balance new-armor new-damage]
              (move-player game-map explored player-x player-y -1 0 player-armor player-damage player-balance treasure-symbol)]
          (if (= new-map game-map)
            (do
              (recur-and-print-map new-map)
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance ))
            (do
              (recur-and-print-map new-map)
              (recur new-map new-explored (dec player-x) player-y player-lives new-armor new-damage new-balance ))))

        (= input "s")
        (let [[new-map new-explored new-balance new-armor new-damage]
              (move-player game-map explored player-x player-y 0 1 player-armor player-damage player-balance treasure-symbol)]
          (if (= new-map game-map)
            (do
              (recur-and-print-map new-map)
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance ))
            (do
              (recur-and-print-map new-map)
              (recur new-map new-explored player-x (inc player-y) player-lives new-armor new-damage new-balance ))))

        (= input "d")
        (let [[new-map new-explored new-balance new-armor new-damage]
              (move-player game-map explored player-x player-y 1 0 player-armor player-damage player-balance treasure-symbol)]
          (if (= new-map game-map)
            (do
              (recur-and-print-map new-map)
              (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance ))
            (do
              (recur-and-print-map new-map)
              (recur new-map new-explored (inc player-x) player-y player-lives new-armor new-damage new-balance ))))

        :else
        (do
          (println-win "\u001b[31mInvalid input. Use w, a, s, or d.\u001b[0m\n")
          (flush)
          
          (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance ))))))

;повторение всей красоты

(def game-map
  (let [initial-game-map (create-empty-map)
        game-map-with-treasures (place-treasures initial-game-map treasure-count treasure-symbol)

        game-map-with-common-weapons (place-treasures game-map-with-treasures common-weapons-count common-weapons-symbol)
        game-map-with-rare-weapons (place-treasures game-map-with-common-weapons rare-weapons-count rare-weapons-symbol)
        game-map-with-legendary-weapons (place-treasures game-map-with-rare-weapons legendary-weapons-count legendary-weapons-symbol)

        game-map-with-common-armor (place-treasures game-map-with-legendary-weapons common-armor-count common-armor-symbol)
        game-map-with-rare-armor (place-treasures game-map-with-common-armor rare-armor-count rare-armor-symbol)
        game-map-with-legendary-armor (place-treasures game-map-with-rare-armor legendary-armor-count legendary-armor-symbol)
    ]
    game-map-with-legendary-armor 
  )
)

(defn wait-for-players []
  (while (< (count @players) max-players)
    (println (str "Waiting for players...  " max-players))
    (Thread/sleep 1000))
  (println "All players are ready! The game is starting..."))


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
        
      
      ;; Комментарий о возможности вызова Thread/sleep для задержки
      ;; (Thread/sleep 3000)
        (add-player {:player-x player-x :player-y player-y :lives player-lives :armor player-armor :damage player-damage :balance player-balance :name @user-name})
        (println "current player: ")
        (println find-player-by-name @players @user-name)
        (flush)
        (wait-for-players)
        (game-loop game-map explored player-x player-y player-lives player-armor player-damage player-balance output-stream))))



(defn handle-connection [input-stream output-stream]
   (swap! connections conj {:input-stream input-stream :output-stream output-stream }) ;добавляем новое соединение в список
  (server-base-fun input-stream output-stream))

(def server (create-server port handle-connection)) 