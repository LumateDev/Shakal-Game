# Команда разработки проекта:

Студенты 34 группы ФКТиПМ.

### Иванов Антон

Осторожно! Он герой! (великий человек, гроза Clojure)

### Емельяненко Александр

"Я успею ещё одну партию сыграть, пока хозяин не пришёл"

### Владарчук Дмитрий

### Сидоренко Максим

### Санкин Юрий

---

- Разрабатываемый проект представляет собой игру по мотивам популярной настольной игры `"Шакал"` (`"Jackal"`).
- Игра предполагает локальный мультиплеер, т.е. игроки будут играть вместе на одной карте, но только в пределах локальной сети.
- Сама игра стоится следующим образом:
  - Пользователи подключаются к серверу, вводят имя для своего персонажа.
  - Все игроки оказываются на одной карте, но в случайных местах.
  - Карту игроки не знают и будут перемещаться по ней на ощуп.
  - На карте в случайных местах будут появляться различные предметы (монетки, оружие), которые игроки могут подобрать;
  - Также игроки могут (и должны) взаимодействовать между собой - игроки могу драться!
  - За победу в бою и за получение монетки игрокам будут начисляться очки.
  - По истечению времени/кол-ва ходов/при достижении лимита очков игра будет окончена, а игроки увидят таблицу лидеров.

---

# План выполнения задач прост и понятен: все должны поучаствовать в реализации каждой детали проекта.

# Для самых маленьких:

Запуск сервера: `./start_server.bat` для Windows и `./start_server.sh` для Linux.
После этого можно открыть новый терминал, подключиться командой: `telnet localhost 5555`.

---
