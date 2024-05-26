:- use_module(library(socket)). %библиотека для взаимодействия с сокетами
:- use_module(library(readutil)). %используется для чтения данных из потока вывода сокета
:- use_module(library(time)). %используется для ожидания

%Подключение к серверу
connect_to_server :-
    setup_call_cleanup(
        tcp_socket(Socket),
        (
          tcp_connect(Socket, localhost:5555),
          tcp_open_socket(Socket, InStream, OutStream),
          handle_connection(InStream, OutStream)
        ),
        tcp_close_socket(Socket)
    ).

%Обработка соединения
handle_connection(InStream, OutStream) :-
    process_server_messages(InStream, OutStream, searching_top_left). %чтение и обработка сообщений сервера

%Чтение строки из входного потока
read_line(Stream, Line) :-
    read_line_to_codes(Stream, Codes),
    string_codes(Line, Codes).

%Отправка строки на сервер
send_line(Stream, Line) :-
    format(Stream, "~w~n", [Line]),
    flush_output(Stream).

%Обработка сообщений сервера
process_server_messages(InStream, OutStream, State) :-
    read_line(InStream, Message),
    process_message(Message, OutStream, State, NewState),
    process_server_messages(InStream, OutStream, NewState).

%Обработка конкретного сообщения
process_message(Message, OutStream, State, NewState) :-
    (
      sub_string(Message, _, _, _, "Waiting for players...") -> 
        format("Received from server: ~w~n", [Message]),
        NewState = State %пропуск сообщения об ожидании игроков
      ;
      sub_string(Message, _, _, _, "Input user name:") -> 
        format("Received from server: ~w~n", [Message]), 
        send_line(OutStream, "LilJuk ak Patrolman"), 
        format("Send to the server: ~w~n", "LilJuk ak Patrolman"),
        NewState = State %ввод никнейма
      ;
      sub_string(Message, _, _, _, "It's your turn now:") -> 
        format("Received from server: ~w~n", [Message]),
        move_bot(State, OutStream, NewState) %перемещение по алгоритму
      ;
      sub_string(Message, _, _, _, "Not your turn!") -> 
        format("Received from server: ~w~n", [Message]),
        NewState = State %пропуск сообщения об ожидании
      ;
      sub_string(Message, _, _, _, "You cannot move outside the island until you collect all the treasures") -> 
        handle_border(State, NewState),
        format("Received from server: ~w~n", [Message]) %обработка границы карты
      ;
      format("Received from server: ~w~n", [Message]),
      NewState = State %обработка других сообщений
    ).

%Выбор направления движения (случайный выбор)
choose_direction(Direction) :-
    random_member(Direction, ["w", "a", "s", "d"]).

%Ожидание на неопределённый интервал
wait_for_seconds :-
    sleep(0.2).

%Обработка границы карты
handle_border(searching_top_left, moving_left).
handle_border(moving_left, moving_up).
handle_border(moving_up, moving_right).
handle_border(moving_right, moving_down).
handle_border(moving_down, moving_left).

%Перемещение бота
move_bot(searching_top_left, OutStream, NewState) :-
    send_line(OutStream, "a"), % двигаться влево
    format("Send to the server: ~w~n", ["a"]),
    wait_for_seconds,
    NewState = searching_top_left.

move_bot(moving_left, OutStream, NewState) :-
    send_line(OutStream, "a"), % двигаться влево
    format("Send to the server: ~w~n", ["a"]),
    wait_for_seconds,
    NewState = moving_left.

move_bot(moving_up, OutStream, NewState) :-
    send_line(OutStream, "w"), % двигаться вверх
    format("Send to the server: ~w~n", ["w"]),
    wait_for_seconds,
    NewState = moving_up.

move_bot(moving_right, OutStream, NewState) :-
    send_line(OutStream, "d"), % двигаться вправо
    format("Send to the server: ~w~n", ["d"]),
    wait_for_seconds,
    NewState = moving_right.

move_bot(moving_down, OutStream, NewState) :-
    send_line(OutStream, "s"), % двигаться вниз
    format("Send to the server: ~w~n", ["s"]),
    wait_for_seconds,
    NewState = moving_down.

%Запуск бота
start_bot :-
    connect_to_server.