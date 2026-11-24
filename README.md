# Радио Няня на Телефоне

Использование смартфона в качестве беспроводного микрофона-«радио-няни».
Телефон (Android) пишет звук с микрофона и передаёт его по UDP на компьютер
(Windows), где звук записывается/воспроизводится. Приложение полностью
повторяет логику работы `pmr.c`: телефон сам отправляет пинги на сервер,
сервер запоминает адрес телефона и шлёт ему команды «слушать/стоп».

---

## Состав проекта

Проект состоит из двух частей:

| Часть | Язык / платформа | Назначение |
|---|---|---|
| `Windows-клиент` (`W10_MicClient`, C# `.NET` / WinForms) | Рабочий стол Windows | UDP-сервер: принимает пинги и аудио, отправляет команды `yes`/`no`, воспроизводит звук |
| `Android-приложение` (`AndroidUdpMic`, Java) | Android (minSdk 26) | UDP-клиент: служит микрофоном, шлёт пинги, принимает команды, передаёт аудио |

---

## Как это работает

1. **Phone → Computer.** Телефон каждые 2 секунды отправляет на сервер
   UDP-пинг `PING` (порт 5254).
2. **Computer запоминает телефон.** Сервер запоминает `IPEndPoint` отправителя
   пинга и считает его активным клиентом.
3. **Computer → Phone.** Когда пользователь нажимает «Слушать», сервер
   отправляет команду `yes` на запомненный адрес телефона.
4. **Phone → Computer (звук).** Получив `yes`, телефон начинает писать
   аудио с микрофона и передавать его UDP-пакетами на сервер.
5. **Computer воспроизводит** полученный звук. Команда `no` останавливает
   передачу.

Формат аудио-пакета: первые 4 байта — заголовок
(`0x15` = WAV_16000_16, канал, `clientId`), далее — полезная нагрузка звука.

---

## Сборка и запуск (Windows-клиент)

Требуется `.NET Framework 4.x` (компилятор `csc.exe` входит в состав .NET).

Один раз скопировать и выполнить в PowerShell:

```powershell
clear; Stop-Process -Name "W10_MicClient" -ErrorAction SilentlyContinue; Start-Sleep -Seconds 1; cd C:\W10_MicClient; C:\Windows\Microsoft.NET\Framework64\v4.0.30319\csc.exe /target:winexe /r:System.Web.Extensions.dll /out:W10_MicClient.exe *.cs; Start-Process "C:\W10_MicClient\W10_MicClient.exe"
```

---

## Сборка Android-приложения и обновление на GitHub

Убедиться, что исходники сохранены в UTF-8 без BOM (иначе Android-компилятор
может упасть), закоммитить и отправить:

```powershell
clear; cd C:\Users\TOSHIBA\AndroidUdpMic; $path = "C:\Users\TOSHIBA\AndroidUdpMic"; Get-ChildItem -Path "$path\*" -Recurse -Include *.java, *.xml, *.gradle, *.properties, *.yml, *.pro | ForEach-Object { $content = [System.IO.File]::ReadAllText($_.FullName); if ($content.Length -gt 0 -and $content[0] -eq [char]0xFEFF) { $content = $content.Substring(1); $utf8NoBom = New-Object System.Text.UTF8Encoding $false; [System.IO.File]::WriteAllText($_.FullName, $content, $utf8NoBom) } }; git add .; git commit -m "V3.0: Server-Client architecture"; git push origin main
```

---

## Настройка и использование

**На телефоне (Android V3.0):**

| Поле | Значение |
|---|---|
| IP-адрес сервера (компьютера) | IP компьютера (показывается в Windows-программе) |
| Порт сервера | `5254` |
| Порт для приёма команд (UDP) | `44444` (оставить по умолчанию) |
| Идентификатор клиента (Client ID) | напр. `10001` |

После сохранения настроек нажать **«Запустить службу в фоне»**.

**На компьютере:** нажать **«Слушать порт 51115»** — сервер отправит команду
`yes` на телефон, и начнётся передача звука.

> Приложение требует разрешения на запись с микрофона
> (`RECORD_AUDIO`) и, на Android 13+, на показ уведомлений
> (`POST_NOTIFICATIONS`).

---

## Технические детали

- Windows-сервер: `UdpServer.cs` (приём пингов/аудио, команды, `event`)
  и `MainForm.cs` (UI, список телефонов, воспроизведение `AudioPlayer`).
- Android-клиент: `UdpNetwork.java` (сокеты, пинги, приём команд, отправка
  аудио), `MainActivity.java` (UI/настройки), фоновый сервис `MicService`.
- Данные телефонов (список) хранятся в `phones.json`.