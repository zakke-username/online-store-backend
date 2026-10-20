# API-dokumentaatio

## Tuotekategoriat

| Metodi | Polku                                   | Kuvaus                                                             | Parametrit/body | Palauttaa                                                                                                                   |
|--------|-----------------------------------------|--------------------------------------------------------------------|-----------------|-----------------------------------------------------------------------------------------------------------------------------|
| GET    | `/api/categories`                       | Kaikki kategoriat id:n mukaan järjestettynä                        | –               | 200: lista kategorioista `{ id, name, description }`                                                                        |
| GET    | `/api/categories/{categoryId}`          | Yksi kategoria                                                     | –               | 200: kategoria `{ id, name, description }`; 404: kategoriaa ei löydy                                                        |
| GET    | `/api/categories/{categoryId}/products` | Kategorian kaikki tuotteet id:n mukaan järjestettynä. Ei sivutettu | –               | 200: lista tuotteista `{ id, name, description, price, stockQuantity, categoryId, categoryName }`; 404: kategoriaa ei löydy |

## Tuotteet

| Metodi | Polku | Kuvaus | Parametrit | Palauttaa |
|---|---|---|---|---|
| GET | `/api/products` | Tuotteet id:n mukaan järjestettynä. Kaikki suodattimet ovat valinnaisia, ja ne yhdistetään AND-ehdolla. Sivutettu | `categoryId`, `q` (hakee nimestä ja kuvauksesta kirjainkoosta ja aksenteista välittämättä), `minPrice`, `maxPrice`, `inStock` (oletus `false`), `page` (0:sta alkaen), `size` (oletus 20, enintään 2000) | 200: sivu `{ content, page: { size, number, totalElements, totalPages } }`, jossa `content` on lista tuotteista `{ id, name, description, price, stockQuantity, categoryId, categoryName }` |
| GET | `/api/products/popular` | Myydyimmät tuotteet: tuotteet, joita on myyty eniten kappaleita viimeisten `days` päivän aikana, myydyin ensin. Peruttuja tilauksia ei lasketa. Ei sivutettu | `days` (oletus 30), `limit` (oletus 10); molempien on oltava positiivisia | 200: lista tuotteista `{ id, name, description, price, stockQuantity, categoryId, categoryName, unitsSold }`; 400: `days` tai `limit` ei ole positiivinen |
| GET | `/api/products/{productId}` | Yksi tuote | – | 200: tuote `{ id, name, description, price, stockQuantity, categoryId, categoryName }`; 404: tuotetta ei löydy |

## Asiakkaat

| Metodi | Polku | Kuvaus | Parametrit | Palauttaa |
|---|---|---|---|---|
| GET | `/api/customers` | Hae asiakas sähköpostiosoitteen perusteella (toistaiseksi kirjautumisen korvike) | `email` (pakollinen) | 200: asiakas `{ id, firstName, lastName, email, phone, version }`; 404: sähköpostiosoitteella ei löydy asiakasta |
| GET | `/api/customers/{customerId}` | Asiakkaan tiedot | – | 200: asiakas `{ id, firstName, lastName, email, phone, version }`; 404: asiakasta ei löydy |
| POST | `/api/customers` | Rekisteröi asiakas | Body: `firstName`, `lastName`, `email` (pakollisia), `phone` (valinnainen) | 201: uusi asiakas `{ id, firstName, lastName, email, phone, version }` ja `Location`-otsake; 400: virheellinen body; 409: sähköpostiosoite on jo käytössä |
| PUT | `/api/customers/{customerId}` | Korvaa nimi, sähköpostiosoite ja puhelinnumero | Body: kuten `POST`, sekä viimeksi luettu `version` (pakollinen) | 200: päivitetty asiakas `{ id, firstName, lastName, email, phone, version }`; 400: virheellinen body tai `version` puuttuu; 404: asiakasta ei löydy; 409: sähköpostiosoite on jo käytössä tai `version` on vanhentunut (asiakasta on muutettu sen jälkeen) |
| DELETE | `/api/customers/{customerId}` | Poista asiakas ja asiakkaan osoitteet | – | 204; 404: asiakasta ei löydy; 409: asiakkaalla on tilauksia |

## Osoitteet

| Metodi | Polku | Kuvaus | Parametrit | Palauttaa |
|---|---|---|---|---|
| GET | `/api/customers/{customerId}/addresses` | Asiakkaan osoitteet id:n mukaan järjestettynä. Ei sivutettu | – | 200: lista osoitteista `{ id, streetAddress, postalCode, city, country, version }`; 404: asiakasta ei löydy |
| GET | `/api/customers/{customerId}/addresses/{addressId}` | Yksi osoite | – | 200: osoite `{ id, streetAddress, postalCode, city, country, version }`; 404: asiakkaalla ei ole tätä osoitetta |
| POST | `/api/customers/{customerId}/addresses` | Lisää osoite | Body: `streetAddress`, `city` (pakollisia), `postalCode`, `country` (valinnaisia) | 201: uusi osoite `{ id, streetAddress, postalCode, city, country, version }` ja `Location`-otsake; 400: virheellinen body; 404: asiakasta ei löydy |
| PUT | `/api/customers/{customerId}/addresses/{addressId}` | Korvaa osoite | Body: kuten `POST`, sekä viimeksi luettu `version` (pakollinen) | 200: päivitetty osoite `{ id, streetAddress, postalCode, city, country, version }`; 400: virheellinen body tai `version` puuttuu; 404: asiakkaalla ei ole tätä osoitetta; 409: osoite on jonkin tilauksen käytössä tai `version` on vanhentunut (osoitetta on muutettu sen jälkeen) |
| DELETE | `/api/customers/{customerId}/addresses/{addressId}` | Poista osoite | – | 204; 404: asiakkaalla ei ole tätä osoitetta; 409: osoite on jonkin tilauksen käytössä |

## Tilaukset

| Metodi | Polku | Kuvaus | Parametrit | Palauttaa |
|---|---|---|---|---|
| GET | `/api/orders/customer/{customerId}` | Asiakkaan tilaukset, uusin ensin. Sivutettu | `status` (valinnainen): `NEW`, `PROCESSING`, `SHIPPED`, `DELIVERED` tai `CANCELLED`; `page` (0:sta alkaen), `size` (oletus 20, enintään 2000) | 200: sivu `{ content, page: { size, number, totalElements, totalPages } }`, jossa `content` on lista tilauksista `{ id, customerId, orderDate, deliveryDate, status, shippingAddress: { id, streetAddress, postalCode, city, country, version }, items: [{ productId, productName, quantity, unitPrice, lineTotal }], total }`; 400: tuntematon `status`; 404: asiakasta ei löydy |
| GET | `/api/orders/customer/{customerId}/{orderId}` | Yksi tilaus tuotteineen, toimitusosoitteineen ja loppusummineen | – | 200: tilaus `{ id, customerId, orderDate, deliveryDate, status, shippingAddress: { id, streetAddress, postalCode, city, country, version }, items: [{ productId, productName, quantity, unitPrice, lineTotal }], total }`, jossa `deliveryDate` on `null`, kunnes tilaus on toimitettu; 404: asiakkaalla ei ole tätä tilausta |
| POST | `/api/orders/customer/{customerId}` | Tee tilaus. Tuotteiden nykyiset hinnat kopioidaan tilaukseen ja varastosaldoa vähennetään | Body: `shippingAddressId`, `items`: lista `{ productId, quantity }` (kaikki pakollisia; vähintään yksi tuote, kukin tuote vain kerran, positiiviset määrät) | 201: uusi tilaus `{ id, customerId, orderDate, deliveryDate, status, shippingAddress: { id, streetAddress, postalCode, city, country, version }, items: [{ productId, productName, quantity, unitPrice, lineTotal }], total }`, jonka `status` on `NEW`, ja `Location`-otsake; 400: virheellinen body, osoite ei kuulu asiakkaalle tai tuotetta ei ole olemassa tai se on listattu kahdesti; 404: asiakasta ei löydy; 409: varastosaldo ei riitä |
| POST | `/api/orders/customer/{customerId}/{orderId}/cancel` | Peru tilaus ja palauta sen tuotteet varastoon | – | 200: peruttu tilaus `{ id, customerId, orderDate, deliveryDate, status, shippingAddress: { id, streetAddress, postalCode, city, country, version }, items: [{ productId, productName, quantity, unitPrice, lineTotal }], total }`; 404: asiakkaalla ei ole tätä tilausta; 409: tilauksen tila ei ole `NEW` tai `PROCESSING` |

## Tilauksen tuotteet

| Metodi | Polku | Kuvaus | Parametrit | Palauttaa |
|---|---|---|---|---|
| GET | `/api/orders/customer/{customerId}/{orderId}/items` | Tilauksen tuotteet tuotteen id:n mukaan järjestettynä. Ei sivutettu | – | 200: lista tilauksen tuotteista `{ productId, productName, quantity, unitPrice, lineTotal }`; 404: asiakkaalla ei ole tätä tilausta |
| POST | `/api/orders/customer/{customerId}/{orderId}/items` | Lisää tuote tilaukseen sen nykyisellä hinnalla ja vähennä määrä varastosaldosta | Body: `productId`, `quantity` (pakollisia; `quantity` positiivinen) | 201: lisätty tuote `{ productId, productName, quantity, unitPrice, lineTotal }` ja `Location`-otsake; 400: virheellinen body tai tuotetta ei ole olemassa; 404: asiakkaalla ei ole tätä tilausta; 409: tilauksen tila ei ole `NEW`, tuote on jo tilauksessa tai varastosaldo ei riitä |
| PUT | `/api/orders/customer/{customerId}/{orderId}/items/{productId}` | Muuta määrää; vain erotus otetaan varastosta tai palautetaan varastoon | Body: `quantity` (pakollinen, positiivinen) | 200: päivitetty tuote `{ productId, productName, quantity, unitPrice, lineTotal }`; 400: virheellinen body; 404: asiakkaalla ei ole tätä tilausta tai tuote ei ole tilauksessa; 409: tilauksen tila ei ole `NEW` tai varastosaldo ei riitä |
| DELETE | `/api/orders/customer/{customerId}/{orderId}/items/{productId}` | Poista tuote tilauksesta ja palauta sen määrä varastoon | – | 204; 404: asiakkaalla ei ole tätä tilausta tai tuote ei ole tilauksessa; 409: tilauksen tila ei ole `NEW` tai tuote on tilauksen viimeinen |
