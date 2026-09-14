# Shrnutí souladu se specifikací

Legenda:

✅ - Splněno

⚠️ - Částečně splněno

❌ - Nesplněno

---

## Příjem dat

> Vystavit endpoint http://host/uploadData

✅ Aplikace vystavuje endpoint pro nahrání na adrese /uploadData, který přijímá CSV soubor.

> Soubor může obsahovat tisíce řádků

✅ Soubor se čte průběžně pomocí streamování a použití Iteratoru, takže velké soubory nezahltí paměť,
a odpověď se vrací ihned, zatímco zpracování pokračuje na pozadí. Mohlo by možná ještě existovat nějake omezení na
velikost souboru,
nebo počet řádků, aby jeden request nevyčerpal celý denní limit API volání, ale to není specifikováno.

> Data je potřeba před uložením validovat a uložit jen validní záznamy

⚠️ Záznamy se před uložením validují a neplatné se přeskakují, takže se ukládají pouze platné záznamy.
Validace je ale nevyvážená a neúplná. Pravidlo pro telefonní číslo je příliš přísné a odmítá řadu legitimních formátů,
zatímco IČO (registrační číslo) a název klienta (title) se vůbec nekontrolují zda jsou vyplněny. Když se pošle regNumber
prázdné,
tak to při zavolání Raynet API vrati internal server error se zprávou "Záznam nelze upravit. Jedná se o vlastní firmu.".
Stejně tak nastane v aplikaci error při nevyplnění title.

> Ukázka dat (regNumber;title;email;phone)

✅ Aplikace čte přesně toto rozložení sloupců se středníkem jako oddělovačem.

---

## Upsert dat do RCRM

> U každého záznamu je potřeba zjistit jestli je již v RCRM evidován.

✅ U každého záznamu aplikace ověří, zda firma v CRM již existuje. Pokud ne, vytvoří ho, pokud ano, tak ho aktualizuje.

> Pokud existuje (zjištěno na základě shody podle IČ (regNumber) bude aktualizován

❌ Existující firmy se sice správně dohledávají podle registračního čísla, ale samotná
aktualizace nefunguje. Aplikace záznam uloží, aniž by kdy aplikovala nové hodnoty z CSV, takže
update ve skutečnosti nic nezmění. Jde o nejzávažnější nedostatek vůči specifikaci.

> Pokud neexistuje bude založen nový klient

⚠️ Noví klienti se podle požadavku zakládají. Informace posílané do CRM jsou však neúplné. Email a telefon z CSV se
nepředávají,
přestože je CRM umí uložit (/company endpoint to umožnuje přijmout). Takže nově založená firma nemá vyplněny tyto
kontaktní údaje.

> Po nahrání všech dat bude odeslán informativní e-mail klientovi o dokončení akce

⚠️ Při dokončení se odešle email o dokončení akce na jednu pevně nakonfigurovanou adresu.
Tento email však vždy hlásí úspěch i tehdy, když se část nebo mnoho záznamů nahrát nepodařilo, což může být zavádějící.

> SMTP server a e-mail bude uveden v konfiguračním souboru

✅ Mailový server i emailové adresy se načítají z konfigurace a nejsou napevno v kódu.

---

## Omezení

> V RCRM může být již uloženo až 200 000 klientů

✅ Dohledání existující firmy je filtrováno podle registračního čísla, které odpovídá IČO od klienta, takže
velikost CRM nezpůsobuje, že by aplikace načítala velké množství dat. Navíc bych očekával, že většina klientů nebude
pod stejným IČO, takže dohledávání bude většinou vracet 0 nebo 1 záznam (případně nějaký menší počet).

> Denně je možné udělat maximálně 24 000 volání API (viz. dokumentace)

⚠️ Aplikace sleduje zbývající limit hlášený CRM a při jeho poklesu pod nastavený threshold přestane
odesílat a zbytek odloží na plánovaný opakovaný pokus. Snaží se tedy limit respektovat. Dvě volání na
záznam (dohledání a poté zápis) jsou minimem bezpečného upsertu vůči tomuto API, jelikož hromadný endpoint
neexistuje a nemužeme si ani ukládat ID od klientů a považovat je za spolehlivé, protože firma mohla být
smazána přímo v UI CRM. Problém ale je, že sledování limitu uložené v proměnné není spolehlivé,
když běží více nahrání současně, a že nic neomezuje počet souběžných spojení do CRM, i když
dokumentace jasně říká, že nesmí být překročen limit 4 současně aktivních spojení (aplikace defaultně umožnuje 8 uploadů
současně).

> Nahrání CSV souboru může probíhat několikrát za den s různě velkými sadami dat

⚠️ Nahrání lze posílat opakovaně a aplikace je příjmá kdykoli nehledě na velikost dat. Problémem je, že
souběžná nebo opakovaná nahrání nejsou dobře koordinována a mohou se navzájem ovlivňovat.

---

## Stack

> Použít Java nebo Kotlin a libovolný vyhovující stack

✅ Aplikace je postavena v Javě na běžném a vhodném technologickém stacku (Spring Boot,
relační databáze a standardní knihovny).
