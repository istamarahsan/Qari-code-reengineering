# Qari

Qari is a discord bot. She can turn text into qr code. 
You can also ask her to remember a qr code under a specific name.
Then, you only need to refer to the name rather than the full text next time you want it.

Commands:
- /qr (text: text to encode to QR)
- /qrsave (text: text to save as QR) (name: name to refer to later)
- /qrload (name: name of a previously-saved QR)

Note that saves are global, not per Discord user.

You can invite her to your server here:\
https://discord.com/api/oauth2/authorize?client_id=1079893597039644723&permissions=0&scope=bot%20applications.commands

If not, there should also be a brief demonstration on presentation day.

Hopefully you now have a good idea of the behavior of this program before you refactor it.

Refactoring Topics:
- Imperative Abstraction
- Missing Abstraction
- Partial Abstraction

Note that you are allowed to create new files, whether that be classes, interfaces, enums or records. 
You may also edit any file as you like, although some parts, like that registers slash commands to discord, 
were not intended to be changed pertaining to the target topics of this refactoring.

In fact, barely any of the code inflicted with smells depends on the Discord4J API. It's actually the other way around.

Have fun :3