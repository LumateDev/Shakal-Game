#!/usr/bin/env clj

(ns server ;простарнство имён server будет использовать пространства имён clojure.contrib.server-socket и clojure.contrib.duck-streams
    (:use [clojure.contrib server-socket duck-streams]) ;server-socket работает с сокетами, а duck-streams работает с потоками ввода-вывода
    (:require [welcome :refer [welcome-in-game]]) ;импортируем функцию-приветствие
    (:require [map :refer [create-empty-map print-map get-random-empty-cell get-static-empty-cell place-player move-player]]) ;импортируем функции для работы с картой
)

(def port 5555) ;будем подключаться к серверу по порту 5555
(def exit-keyword "exit") ;кодовое слово для отключения с сервера
(def game-map (create-empty-map)) ;создаём пустую карту

(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))

(defn server-base-fun [input-stream output-stream] ;основная функция, которая делает всё
    (let [[player-x player-y] (get-random-empty-cell game-map)] ;задаём точку спавна персонажу
        (place-player game-map player-x player-y) ;помещаем персонажа
        (println-win "Player initialized. Waiting for input...") ;для отладки
        (binding [*in* (reader input-stream) *out* (writer output-stream)] ;биндим потоки ввода-вывода
            (welcome-in-game) ;приветствие
            (flush) ;очистка буфера вывода
            (loop [game-map game-map player-x player-x player-y player-y] ;продублированные аргументы для того, чтобы их было чётное количество (просто нужно для loop)
                (print-map-with-player game-map player-x player-y) ; используем новую функцию для вывода карты с игроком
                (flush) ;очистка буфера вывода
                (let [input (read-line)] ;запуск цикла ввода
                    (cond ;условия
                        (= input exit-keyword) ;если пробуем выйти
                            (do
                                (println-win "Thank you for playing the Jackal Game!")
                                (flush)
                                (.close output-stream)) ;то выходим (Шерлок?)
                        (= input "w") (let [new-map (move-player game-map player-x player-y 0 -1)]
                            (if (= new-map game-map) (recur game-map player-x player-y)
                                                     (recur new-map player-x (dec player-y))))
                        (= input "a") (let [new-map (move-player game-map player-x player-y -1 0)]
                            (if (= new-map game-map) (recur game-map player-x player-y)
                                                     (recur new-map (dec player-x) player-y)))
                        (= input "s") (let [new-map (move-player game-map player-x player-y 0 1)]
                            (if (= new-map game-map) (recur game-map player-x player-y)
                                                     (recur new-map player-x (inc player-y))))
                        (= input "d") (let [new-map (move-player game-map player-x player-y 1 0)]
                            (if (= new-map game-map) (recur game-map player-x player-y)
                                                     (recur new-map (inc player-x) player-y)))
                        :else ;если пользователь не попадает ложкой в рот с первой попытки
                            (do
                                (println-win "Invalid input. Use w, a, s, or d.") ;инвалид ввод
                                (flush)
                                (recur game-map player-x player-y)))))))) ;повторный вызов этой красоты

(def server (create-server port server-base-fun)) ;запуск сервера на порту
