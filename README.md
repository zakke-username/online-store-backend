# Verkkokaupan taustapalvelu

Tietokantaratkaisut-kurssin projektityö.

## Rajapinnan tavoite ja laajuus

Tämän tietokantaratkaisut-kurssin projektityön tavoite on toteuttaa toimiva Java/Spring Boot -pohjainen demo web-taustapalvelusta ja APIsta. 

Tämä rajapinta palvelee lähinnä loppukäyttäjän näkökulmasta verkkokauppaa, ja olennaiset siihen liittyvät päätepisteet on toteutettu. Projektissa on tietoisesti, tehtävänannon mukaisesti, jätetty pois selvästi ylläpidollisia päätepisteitä ja toiminnallisuuksia (esimerkiksi tuotekatalogiin lisääminen ja poistaminen), jotka eivät ole verkkokaupan asiakkaalle relevantteja. Ohjeistuksen mukaisesti myöskään kirjautumista (authentication/authorization) ei ole toteutettu: todellisuudessa asiakkaat tietenkin todentaisivat itsensä päästäkseen päätepisteiden resursseihin käsiksi.

Projektin keskeinen tarkoitus on kuitenkin demonstroida tietokantojen suunnittelua ja niiden sujuvaa käyttöä taustapalvelun pohjalla, mikä on toteutettu tehtävänannon mukaisesti. Spring-frameworkiin liittyviä tietokantakirjastoja ja olio-relaatiomallinnusta on käytetty tehokkaasti ja oikeaoppisesti. Taustapalvelu on REST-pohjainen ja se toteuttaa olennaiset ja vaaditut CRUD-päätepisteet.

**Päätepisteiden dokumentaatio: [API_DOC.md](API_DOC.md)**

## Tietokanta

Taustapalvelu käyttää paikallisesti toimivaa MariaDB-relaatiotietokantaa. Tietokannan skeema pohjautuu kurssimateriaaleissa ja projektikansion dokumenteista löytyvään skriptiin. Tietokannassa ja sen suunnittelussa on hyödynnetty useita kurssilla läpi käytyjä ominaisuuksia ja toiminnallisuuksia, kuten indeksointia, transaktioita, samanaikaisuuden hallintaa, näkymiä, proseduureja ja liipaisimia.

Tietokannan nimi on `online_store` ja sovelluskäyttäjä on `online_store_app`.

Tietokannan SQL-skriptit:
- **Taulut ja toiminnallisuudet:** [schema.sql](src/main/resources/schema.sql)
- **Testidata:** [data.sql](src/main/resources/data.sql)

### Relaatiokaavio

[kuva]

### Indeksointi

MariaDB indeksoi automaattisesti pääavaimet tauluissa, mikä nopeuttaa huomattavasti jo suurta osaa kyselyistä. Kolme indeksiä on kuitenkin lisätty nopeuttamaan kyselyjä. Nämä joko nopeuttavat kyselyjä (testattu EXPLAIN-lauseella), tai voisivat olla tulevaisuudessa teoriassa hyödyllisiä, mikäli projektia ja tietokantaa laajennettaisiin oikeaan käyttöön.

Lisätyt indeksit (schema.sql-skriptissä):

- Orders (customer_id, order_date): tehostaa asiakkaan tilaushistorian hakemista. Tilaushistoria haetaan nyt kyselyllä, joka järjestää valmiiksi tietueet aikaleiman/päivämäärän mukaan. Ennen tätä ohjelmistojen tuli hakea kaikki asiakkaan tilaukset ja järjestää ne jälkikäteen. 
- Orders (order_date): hyödyllinen, mikäli halutaan listata myydyimmät tuotteet. Näin voidaan helposti esimerkiksi rajata viimeisimmän 30 päivän tulokset, ilman, että koko taulukko täytyy skannata.
- Orders (status, order_date): tulevaisuuden kannalta voisi olla erittäin hyödyllistä saada nopeasti näkyviin tilaukset niiden nykyisen tilan avulla. Voitaisiin luoda kyselyjä, jotka tämän indeksin avulla järjestettäisiin myös tilauspäivämäärän perusteella.

### Transaktiot

Rajapinta toteuttaa pyynnöt transaktioiden avulla lähes jokaisessa tapauksessa. Spring-annotaatiot  kommunikoivat tämän tietokannalle (@Transactional). Näin vältetään virhetiloja ja estetään samanaikaisuudesta johtuvia ongelmia.

Esimerkkejä, joissa transaktioita on hyödynnettu projektissa:

- **Uuden tilauksen luominen tulee olla atomista.** Projektissa yksi transaktio hakee asiakkaan tiedot, osoitetiedot, tuotetiedot, lisää uuden tilauksen ja tuotteet siihen, päivittää saatavilla olevan määrän (stock). Mikäli virhetila syntyy tai jokin näistä operaatioista epäonnistuu, koko transaktio-operaatio peruuntuu eikä ristiriitaista tilaa synny tietokantaan.
- Tilauksien peruutus on myös atominen operaatio. Tilauksen statuksen muutos ja tuotteen palauttaminen saataville tapahtuvat samaan aikaan.
- Kun vanhentuneet tilaukset peruutetaan automaattisesti, tapahtuu tämä myös transaktion sisällä: kaikkien näiden tilausten status muuttuu kerralla.

### Samanaikaisuus

**CHECK-rajoitteilla** on varmistettu tietokantatasolla eri operaatioiden turvallisuutta. Tärkeä esimerkki tästä on stock_quantity: tietokanta tarkistaa, että tuotteita on vähintään 0 kappaletta saatavilla. Tämä estää negatiivisen määrän tässä kohtaa. Samalla periaatteella myös tilauksen tuotteiden määrän tulee olla yli 0, tuotteen hinnan tulee olla vähintään 0, ja tietokanta myös varmistaa tilauksen statuksen olevan validi vaihtoehto.

**Versiointia** on käytetty asiakkaan päivittäessä henkilötietojaan. Koska esimerkiksi lomakkeen täyttäminen voi kestää pitkään, ei tässä tilanteessa ole voitu käyttää lukkoja.

Samanaikaisia pyyntöjä on myös estetty **lukoilla**, joita pidetään kunnes transaktio on suoritettu onnistuneesti. Operaatiot jotka muokkaavat saatavilla olevien tuotteiden määrää tai tilauksia lukitaan ensin, ja sen jälkeen tehdään muutoksia. Näin esimerkiksi kaksi asiaksta eivät voi ostaa samaa viimeistä tuotetta samaan aikaan. Lukkojen eri tyyppejä (esimerkiksi `@Lock(LockModeType.PESSIMISTIC_WRITE)`) on spesifioitu repository-luokissa.

### Näkymät

Näkymä on tietokantaan tallennettu SELECT-kysely, jota voi kysellä kuin taulua. Näkymä ei ole fyysinen taulu eikä sisällä omaa dataa, vaan kysely kohdistetaan aina "oikeisiin" tauluihin ja ajantasaiseen dataan. Näkymillä esimerkiksi monimutkaiset liitokset voidaan piilottaa yhden nimen taakse, ja niillä voidaan rajata, mitä tietoja käyttäjä näkee.

Tässä Projektissa näkymiä käytetään yksinkertaistamaan usein toistuvia kyselyjä ja rajaamaan asiakkaalle näytettävät tuotetiedot.

Esimerkkejä näkymistä (schema.sql-skriptissä):

- **`ProductCatalog`** yhdistää tuotteet ja niiden kategorian nimen (Products ja ProductCategories), ja siinä on vain asiakkaalle näytettävät kentät: toimittajan tiedot jäävät pois. Myydyimpien tuotteiden päätepiste (`GET /api/products/popular`) hakee tuotetiedot tämän näkymän kautta.
- **`OrderTotals`** laskee jokaiselle tilaukselle tuotteiden kappalemäärän ja loppusumman (Orders ja OrderItems). Sen avulla voisi esimerkiksi hakea asiakkaan tilaukset summineen tai laskea kuukausittaisen myynnin yksinkertaisella kyselyllä.

### Proseduurit

Proseduuri on tietokantaan tallennettu sarja SQL-kyselyjä, jota kutsutaan termillä CALL.  Proseduuri toimii samalla tavalla kaikille kutsujille, esimerkiksi rajapinnalle, asiakasohjelmalle tai ajastetulle tapahtumalle.

Projektissa tilauksen peruutus on toteutettu proseduurina, koska sitä tarvitsevat sekä rajapinta että vanhentuneiden tilausten automaattinen peruutus.

Esimerkkejä toteutetuista proseduureista:

- **`cancel_order`** peruuttaa tilauksen vain, jos se kuuluu asiakkaalle ja sen tila on NEW tai PROCESSING, ja palauttaa sen jälkeen tilauksen tuotteet varastoon. Tarkistus ja muutos tehdään yhdellä UPDATE-lauseella, joten kaksi samanaikaista peruutusta eivät voi palauttaa tuotteita varastoon kahdesti. Rajapinnan cancel-päätepiste kutsuu tätä proseduuria saman transaktion sisällä.
- **`cancel_expired_orders`** käy läpi tilaukset, jotka ovat olleet NEW-tilassa annetun määrän päiviä, lukitsee ne ja peruuttaa ne yksi kerrallaan `cancel_order`-proseduurin avulla.

### Liipaisimet

Liipaisin on tauluun liitetty SQL-kysely, jonka tietokanta suorittaa automaattisesti, kun sen "laukaiseva" tapahtuma toteutuu, esimerkiksi päivitys- tai poistokysely. Liipaisin suoritetaan saman transaktion sisällä kuin sen käynnistänyt lause, joten sen muutokset tallentuvat tai peruuntuvat yhdessä muun muutoksen kanssa.

Projektissa liipaisimilla pidetään tietoja automaattisesti ajan tasalla esimerkiksi seuraavissa tilanteissa:

- **`trg_orders_delivery_date`** asettaa tilauksen toimituspäivän (delivery_date), kun tilauksen tila muuttuu DELIVERED-tilaan.
- **OrderLog-taulun liipaisimet:** kun tilaus luodaan, sille lisätään rivi OrderLog-tauluun. Kun tilauksen tuotteita lisätään, muutetaan tai poistetaan, liipaisimet laskevat tilauksen loppusumman lokiin uudelleen. Näin lokissa on aina tilauksen ajantasainen summa ja luontiaika.

### Tapahtumat

Ajastettu tapahtuma on tietokantaan tallennettu SQL-kysely, jonka tietokanta suorittaa itsenäisesti ja/tai säännöllisesti. Tapahtumilla voidaan hoitaa esimerkiksi ylläpitotehtäviä ilman erillistä sovelluksen ajastinta. 

Projektissa tapahtumaa käytetään hylättyjen tilausten automaattiseen poistamiseen, jotta niihin varatut tuotteet palautuvat takaisin myyntiin:

- **`expire_orders`** suoritetaan kerran tunnissa. Se peruuttaa tilaukset, jotka ovat olleet NEW-tilassa yli 7 päivää, ja palauttaa niiden tuotteet varastoon kutsumalla `cancel_expired_orders`-proseduuria.
- Tapahtuma avaa itse transaktion (START TRANSACTION ... COMMIT), joten yksi ajo peruuttaa joko kaikki vanhentuneet tilaukset tai ei yhtään. Ajon aikana tilaukset ovat lukittuina, joten asiakas ei voi muuttaa tilausta samaan aikaan, kun sitä peruutetaan.