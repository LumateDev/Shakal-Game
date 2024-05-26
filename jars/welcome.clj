
(defn println-win [s] ;функция, которая корректно добавляет символ переноса строк для Windows
    (.write *out* s)
    (.write *out* "\r\n")
    (.flush *out*)
)

(defn welcome-in-game [] ;функция-приветствие
    (println-win "Hi!")
    (println-win "Welcome to \u001b[33mShakals Game\u001b[0m!")
    (println-win "Moving: FORWARD - \u001b[32mw\u001b[0m, LEFT - \u001b[32ma\u001b[0m, BACKWARD - \u001b[32ms\u001b[0m, RIGHT - \u001b[32md\u001b[0m.")
    (println-win "Enter \u001b[32m<exit>\u001b[0m to exit.")
    (println-win "\n") ; Визуально отделил 

    (println-win "\t\u001b[46m\u001b[36;1mX\u001b[0m - It's you")
    (println-win "\t\u001b[41m\u001b[30;1mX\u001b[0m - These are the enemies")
    (println-win "\t\u001b[47m\u001b[32m$\u001b[0m - These are treasures")
    (println-win "\t\u001b[47m\u001b[30;1m!\u001b[0m - This is a common weapon (+1 damage)")
    (println-win "\t\u001b[42m\u001b[37;1m|\u001b[0m - This is a rare weapon (+2 damage)")
    (println-win "\t\u001b[45m\u001b[33;1m}\u001b[0m - This is a legendary weapon (+3 damage)")
    (println-win "\t\u001b[47m\u001b[30;1m^\u001b[0m - This is a common armor (+1 armor)")
    (println-win "\t\u001b[42m\u001b[37;1m)\u001b[0m - This is a rare armor (+2 armor)")
    (println-win "\t\u001b[45m\u001b[33;1m]\u001b[0m - This is a legendary armor (+3 armor)")
    (println-win "\n") ; Визуально отделил
) 


;; (defn user-name-reader [user-name]  ; Функция для получения имени пользователя
;;     (println-win "Input user naem: ")
;;     (let [input (read-line)]
;;         (:= user-name input)
;;     )
;;     [user-name]
;; )

(defn user-name-reader [user-name] ; Более крутая через Atom функция для получения имени пользователя
      (println "Input user name: ")
      (reset! user-name (read-line))
      @user-name
)