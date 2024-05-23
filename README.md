# Команда разработки проекта:

Студенты 34 группы ФКТиПМ.

### Иванов Антон `Хозяин`

Осторожно! Он герой! (великий человек, гроза Clojure)

### Емельяненко Александр `говорит, что он тимлид`

"Я успею ещё одну партию сыграть, пока хозяин не пришёл"
`©Санечка`

Чтобы Саня не переживал и все знали: ЭТО ОН ПРИДУМАЛ ИДЕЮ ШАКАЛА

### Владарчук Дмитрий ![1](https://img.icons8.com/?size=24&id=17836&format=png)

Приемник Подколзина, чел андроед делает не трогайте его

"MVVM это model модель model"
`©Владаргук`

### Сидоренко Максим

Гейм дизайнер игры Шакал, и просто какой-то нелепый шакал

### Санкин Юрий

ак `иногда работает`
ак `джаваскрипт объект нотация`

Не частый гость в универе, он тут сокеты все намутил

"За докер пояснять не буду, потому что это база"
`©Фулей`

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

# Итого

### План выполнения задач прост и понятен. Все должны поучаствовать в реализации каждой детали проекта.

# Для самых маленьких:

Запуск сервера: `./start_server.bat` для Windows и `./start_server.sh` для Linux.
После этого можно открыть новый терминал, подключиться командой: `telnet localhost 5555`.

---

# Устройство сети

![1](https://github.com/LumateDev/Shakal-Game/blob/master/net-highlevel.png)

## Ход работы сервера:
Сервер представлен в виде программы на clojure. Принимает подключения с помощью telnet на порту 5555. 

1. Сервер инициализируется.
2. Сервер открывает сокеты на порту 5555.
3. Сервер ожидает подключения игроков и назначает им уникальные имена.
4. Сервер инициализирует карту.
5. Сервер обрабатывает входящие сообщения и передает им данные о карте, врагах, балансе и игроке.
  1. Если ход игрока, то сервер, прерывает работу, ожидает сообщения от клиента,  обновляет карту, местоположение игрока и остальные изменившиеся данные от игрока на основании измененных данных, или передает ошибку в случае неверно введенных данных и возвразается к моменту ожидания сообщения.
  2. Eсли не ход игрока, то сервер обрабатывает сообщение и передает ему данные о карте, врагах, балансе и игроке с помощью двухстороннего соединения, прерывая работу до подтверждения сообщения со стороны клиента.
6. 
  1. Если игра не закончилась, сервер возвращается к пункту 5
  2. Иначе сервер завершает работу.

### Рекомендации к разработке
1. декомпозировать функции как минимум до уровня каждых пунктов в **ходе работы сервера**
2. сделать общий конфиг файл с параметрами **config.clj**

## Ход работы клиента
 Сервер представлен ввиде терминала. Принимает подключения с помощью telnet на порту 5555.

1.  Клиент открывает порт 5555.
2.  Клиент подключается к серверу.
3.  Клиент задает уникальное имя.
4.  Клиент ожидает сообщения от сервера о начале игры.
5.  Клиент ожидает сообщения от сервера о ходе игры.
  1. Если ход игрока, то клиент передает серверу евент сообщение о ходе.
  2. Если не ход игрока, то сервер передает клиенту данные и текущем состоянии игры.
  3. Если сообщение от сервера о конце игры, то все ломается, ахахах.

### Рекомендации к управлению

пока не наблюдается никаких рекомендации к управлению.
