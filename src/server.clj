#!/usr/bin/env clj

(ns server ;простарнство имён server будет использовать пространства имён clojure.contrib.server-socket и clojure.contrib.duck-streams
    (:use [clojure.contrib server-socket duck-streams]) ;server-socket работает с сокетами, а duck-streams работает с потоками ввода-вывода
    (:require [welcome :refer [welcome-in-game]]) ;импортируем функцию-приветствие
    (:require [map :refer [create-empty-map print-map get-random-empty-cell get-static-empty-cell place-player
                           initialize-explored update-explored move-player print-map-with-explored]]) ;импортируем функции для работы с картой
)

(def port 5555) ;будем подключаться к серверу по порту 5555
(def exit-keyword "exit") ;кодовое слово для отключения с сервера
(def game-map (create-empty-map)) ;создаём пустую карту

(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))

(defn server-base-fun [input-stream output-stream] ;основная функция, которая делает всё
    (let [game-map (create-empty-map) ;создание пустой карты
         [player-x player-y] (get-random-empty-cell game-map) ;получаем клетку для игрока
         game-map (place-player game-map player-x player-y) ;спавним игрока
         explored (update-explored (initialize-explored (count game-map)) player-x player-y 1)] ;обновляем данные о карте
        (println "Player initialized. Waiting for input...") ;для отладки
        (binding [*in* (reader input-stream) *out* (writer output-stream)] ;биндим потоки ввода/вывода
            (welcome-in-game) ;приветствие
            (flush) ;очистка буфера
            (loop [game-map game-map
                   explored explored
                   player-x player-x
                   player-y player-y]
                (print-map-with-explored game-map explored) ;печать карты
                (flush)
                (let [input (read-line)] ;считываем ввод
                    (cond
                        (= input "exit") ;если пытаемся выйти
                            (do
                                (println-win "Thank you for playing the Jackal Game!")
                                (flush)
                                (.close output-stream)) ;то выходим (Шерлок?)
                        (= input "w") ;иначе переходим куда-то
                            (let [[new-map new-explored] (move-player game-map explored player-x player-y 0 -1)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y)
                                    (recur new-map new-explored player-x (dec player-y))))
                        (= input "a") 
                            (let [[new-map new-explored] (move-player game-map explored player-x player-y -1 0)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y)
                                    (recur new-map new-explored (dec player-x) player-y)))
                        (= input "s") 
                            (let [[new-map new-explored] (move-player game-map explored player-x player-y 0 1)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y)
                                    (recur new-map new-explored player-x (inc player-y))))
                        (= input "d") 
                            (let [[new-map new-explored] (move-player game-map explored player-x player-y 1 0)]
                                (if (= new-map game-map) (recur game-map explored player-x player-y)
                                    (recur new-map new-explored (inc player-x) player-y)))
                        :else ;если пользователь не попадает ложкой в рот с первой попытки
                            (do
                                (println-win "Invalid input. Use w, a, s, or d.") ;то предупреждение
                                (flush)
                                (recur game-map explored player-x player-y)))))))) ;повторение всей красоты

(def server (create-server port server-base-fun)) ;запуск сервера на порту
