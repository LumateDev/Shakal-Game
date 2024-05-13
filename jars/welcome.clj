
(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*))

(defn welcome-in-game [] ;функция-приветствие
    (println-win "Hi!")
    (println-win "Welcome to Shakals Game!")
    (println-win "Moving: FORWARD - w, LEFT - a, BACKWARD - s, RIGHT - d.")
    (println-win "Enter <exit> to exit."))
