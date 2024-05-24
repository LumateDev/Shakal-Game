#!/usr/bin/env clj

(ns server ;простарнство имён server будет использовать пространства имён clojure.contrib.server-socket и clojure.contrib.duck-streams
    (:use [clojure.contrib server-socket duck-streams]) ;server-socket работает с сокетами, а duck-streams работает с потоками ввода-вывода
    (:require [welcome :refer [welcome-in-game]]) ;импортируем функцию-приветствие
    (:require [map :refer [create-empty-map print-map get-random-empty-cell get-static-empty-cell place-player
                           initialize-explored update-explored move-player print-map-with-explored place-treasures start-game]]) ;импортируем функции для работы с картой
    ; (:import [java.util Timer TimerTask])
)

(def port 5555) ;будем подключаться к серверу по порту 5555
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

(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))

(defn server-base-fun [input-stream output-stream] ;основная функция, которая делает всё
    (let [initial-game-map (create-empty-map) ;создание пустой карты
          game-map-with-treasures (place-treasures initial-game-map treasure-count treasure-symbol) ;разместить сокровища на карте

          game-map-with-common-weapons (place-treasures game-map-with-treasures common-weapons-count common-weapons-symbol) ; разместить обычные оружия на карте
          game-map-with-rare-weapons (place-treasures game-map-with-common-weapons rare-weapons-count rare-weapons-symbol) ; разместить редкие оружия на карте
          game-map-with-legendary-weapons (place-treasures game-map-with-rare-weapons legendary-weapons-count legendary-weapons-symbol) ; разместить легендарные оружия на карте

          game-map-with-common-armor (place-treasures game-map-with-legendary-weapons common-armor-count common-armor-symbol) ; разместить обычныую броню на карте
          game-map-with-rare-armor (place-treasures game-map-with-common-armor rare-armor-count rare-armor-symbol) ; разместить редкую броню на карте
          game-map-with-legendary-armor (place-treasures  game-map-with-rare-armor legendary-armor-count legendary-armor-symbol) ; разместить легендарную броню на карте  

          [player-x player-y] (get-random-empty-cell game-map-with-legendary-armor) ;получаем клетку для игрока
          game-map (place-player game-map-with-legendary-armor player-x player-y) ;спавним игрока
          explored (update-explored (initialize-explored (count game-map)) player-x player-y 1) ;обновляем данные о карте
          player-balance 0] ;обновляем данные о balance
        (println "Player initialized. Waiting for input...") ;для отладки
        (binding [*in* (reader input-stream) *out* (writer output-stream)] ;биндим потоки ввода/вывода
            (welcome-in-game) ; приветствие

            (user-name-reader user-name) ; Запрашиваем ник пользователя

            ;; Я ТАК И НЕ СМОГ СДЕЛАТЬ ЗАДЕРЖКУ В 3 СЕКУНДЫ ДЛЯ ПРОЧТЕНИЯ ПАМЯТКИ ИГРОКА В НАЧАЛЕ, МОЖЕТ КТО СМОЖЕТ РАЗОБРАТЬСЯ
            ;; ЧЕКНИТЕ ИМПОРТЫ ЗАКОМЕНЧЕНЫЕ СВЕРХУ ЕЩЁ ЕСЛИ ПРОБОВАТЬ БУДЕТЕ

            ;; (Thread/sleep 3000)

            ;; (java.util.Timer.) ; создаете новый таймер
            ;; (.schedule
            ;;     ( java.util.TimerTask. 
            ;;         {
            ;;         :run #(do (flush) )
            ;;         }
            ;;     )
            ;;     3000 ; устанавливаете задержку в миллисекундах
            ;; ) ; ожидание 3 секунды для прочтения информации

            ;; (let [task (proxy [java.util.TimerTask] [] (run [] (flush)))
            ;;     timer (java.util.Timer.)
            ;;     delay 3000]
            ;; (.schedule timer task (long delay)))

            ;; (let [timer (Timer.) 
            ;;     task (proxy [TimerTask] []
            ;;         (run []
            ;;             (println "Waiting for 3 seconds...")
            ;;             (Thread/sleep 3000)))]
            ;; (.schedule timer task 0))

            ;; (let [task (proxy [TimerTask] [] (run [] (Thread/sleep 3000)))  
            ;;     timer (Timer.)]
            ;; (.schedule timer task 0))

            ;; (println "Waiting for 3 seconds...")
            ;; (Thread/sleep 3000)


            (flush) ;очистка буфера
            (loop [game-map game-map
                   explored explored
                   player-x player-x
                   player-y player-y
                   player-lives player-lives
                   player-armor player-armor
                   player-damage player-damage
                   player-balance player-balance] ; новая использующая имя старой
                (print "NICK: ")
                (print-user-name @user-name)   
                (println-win "\u001b[32;1mYour stats:\u001b[0m")
                (print-lives player-lives) ; печать HP перед печатью карты
                (print-armor player-armor) ; печать очков брони перед печатью карты
                (print-damage player-damage) ; печать очков урона перед печатью карты
                (print "\n")
                (println-win "\u001b[33;1mYour inventory:\u001b[0m")
                (print-current-armor player-armor) ; печать надетой брони
                (print-current-weapon player-damage) ; печать текущего оружия
                (print-balance player-balance) ; печать баланса перед печатью карты
                (print "\n") ; визуально отделяем карту от статов  
                (print-map-with-explored game-map explored) ;печать карты
                (flush)
                (let [input (read-line)] ;считываем ввод
                    (cond
                        (= input "exit") ;если пытаемся выйти
                            (do
                                (println-win "\u001b[32mThank you for playing the Jackal Game!\u001b[0m\n")
                                (flush)
                                (.close output-stream)) ;то выходим (Шерлок?)
                        (= input "w") ;иначе переходим куда-то
                            (let [[new-map new-explored new-balance new-armor new-damage] (move-player game-map explored player-x player-y 0 -1 player-armor player-damage player-balance treasure-symbol)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance)
                                    (recur new-map new-explored player-x (dec player-y) player-lives new-armor new-damage new-balance)))
                        (= input "a") 
                            (let [[new-map new-explored new-balance new-armor new-damage] (move-player game-map explored player-x player-y -1 0 player-armor player-damage player-balance treasure-symbol)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance)
                                    (recur new-map new-explored (dec player-x) player-y player-lives new-armor new-damage new-balance)))
                        (= input "s") 
                            (let [[new-map new-explored new-balance new-armor new-damage] (move-player game-map explored player-x player-y 0 1 player-armor player-damage player-balance treasure-symbol)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance)
                                    (recur new-map new-explored player-x (inc player-y) player-lives new-armor new-damage new-balance)))
                        (= input "d") 
                            (let [[new-map new-explored new-balance new-armor new-damage] (move-player game-map explored player-x player-y 1 0 player-armor player-damage player-balance treasure-symbol)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance) 
                                    (recur new-map new-explored (inc player-x) player-y player-lives new-armor new-damage new-balance)))
                        :else ;если пользователь не попадает ложкой в рот с первой попытки
                            (do
                                (println-win "\u001b[31mInvalid input. Use w, a, s, or d.\u001b[0m\n") ;то предупреждение
                                (flush)
                                (recur game-map explored player-x player-y player-lives player-armor player-damage player-balance)))))))) ;повторение всей красоты

(def server (create-server port server-base-fun)) ;запуск сервера на порту
