#!/usr/bin/env clj

(ns server ;простарнство имён server будет использовать пространства имён clojure.contrib.server-socket и clojure.contrib.duck-streams
    (:use [clojure.contrib server-socket duck-streams]) ;server-socket работает с сокетами, а duck-streams работает с потоками ввода-вывода
    (:require [welcome :refer [welcomeInGame]]) ;импортируем функцию-приветствие
)

(def port (* 5 1111)) ;будем подключаться к серверу по порту 5555 

(defn server_base_fun [input_stream output_stream] ;основная функция, которая будет отвечать за ввод/вывод
    (binding [*in* (reader input_stream) *out* (writer output_stream)] ;биндим потоки ввода/вывода
        (welcomeInGame) ;вызываем функцию-приветствие
        (loop [] ;запускаем бесконечный цикл
            (println (read-line)) ;печатает полученную строку от пользователя
            (recur) ;рекурсивный вызов себя же
        )
    )
)

(def server (create-server port server_base_fun)) ;запускаем сервер на порту и указываем функцию для выполнения
