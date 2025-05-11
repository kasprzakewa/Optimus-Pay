# Optimus Price

<img align="right" width="600" src="https://github.com/kasprzakewa/Optimus-Price/blob/main/docs/optimus.png" />

Projekt stanowi implementację algorytmu optymalizującego wybór metod płatności dla zestawu zamówień w internetowym supermarkecie. Celem jest maksymalizacja łącznego rabatu uzyskanego przez klienta, przy pełnym opłaceniu wszystkich zamówień i zgodności z ograniczeniami budżetowymi.

Interaktywna dokumentacja projektu dotępna jest [tutaj](https://kasprzakewa.github.io/Optimus-Price/).

### Kluczowe założenia:

* Każde zamówienie może zostać opłacone:

  * jedną metodą tradycyjną (kartą),
  * w całości punktami lojalnościowymi,
  * częściowo punktami i częściowo metodą tradycyjną.
* Rabaty przypisane do kart są stosowane tylko przy pełnej płatności daną kartą.
* Rabat za punkty (10%) przysługuje przy pokryciu nimi min. 10% wartości zamówienia (brak możliwości łączenia z rabatem za kartę).
* Rabat za pełną płatność punktami zależy od zdefiniowanego procentu dla metody „PUNKTY”.

### Podejście:

Implementacja opiera się na trzech głównych etapach:

1. **Wygenerowanie wariantów płatności** – dla każdego zamówienia tworzony jest zbiór możliwych scenariuszy płatności, uwzględniających dostępne metody, przypisane promocje, rabaty i limity.
2. **Optymalizacja** – spośród wszystkich kombinacji wariantów wybierane jest rozwiązanie maksymalizujące łączny rabat, z użyciem solvera z biblioteki Google OR-Tools.
3. **Dystrybucja punktów lojalnościowych** – po optymalizacji, dla wariantów płatności z wykorzystaniem punktów lojalnościowych, stosowane jest podejście mające na celu zwiększenie udziału punktów, jeśli to możliwe, bez zmiany przyznanego rabatu.

---

## Generowanie wariantów płatności dla zamówienia

Dla każdego zamówienia o określonej wartości, system generuje możliwe warianty płatności, biorąc pod uwagę dostępne metody płatności oraz dostępne promocje. Każdy wariant płatności jest kombinacją różnych metod płatności oraz przypisanych do nich kwot.

1. **Warianty z kartą promocyjną** – jeśli zamówienie może być opłacone kartą promocyjną, system oblicza kwotę, która musi zostać zapłacona, uwzględniając rabat i tworzy wariant tej płatności.

2. **Warianty z punktami lojalnościowymi** – jeśli dostępna jest opcja płatności punktami lojalnościowymi, system rozważa dwa przypadki:

   1. **Płatność za całe zamówienie punktami** – jeśli punkty lojalnościowe wystarczają na pokrycie całej kwoty zamówienia, wariant jest tworzony z uwzględnieniem rabatu za opłacenie całego zamówienia punktami.
   2. **Częściowa płatność punktami (10% wartości zamówienia) + tradycyjna metoda płatności** – jeśli punkty lojalnościowe mogą pokryć co najmniej 10% wartości zamówienia, a pozostałą część można zapłacić tradycyjną metodą, system generuje też taki wariant.

3. **Warianty płatności metodą bez rabatu** – system rozważa również warianty, w których zamówienie opłacane jest wyłącznie tradycyjną metodą płatności, bez żadnych rabatów.

### Podsumowanie

Dla każdego zamówienia, system generuje możliwe warianty płatności, uwzględniając dostępne metody płatności, rabaty oraz limity dla punktów lojalnościowych. Tworzy to przestrzeń poszukiwań dla solvera, który może przeanalizować wszystkie dostępne opcje i znaleźć optymalne rozwiązanie.

---

## Model programowania liniowego – maksymalizacja rabatu

Model programowania liniowego ma na celu znalezienie optymalnego rozdziału metod płatności dla zamówień w taki sposób, aby maksymalizować łączny rabat, jednocześnie przestrzegając ograniczeń dotyczących dostępnych metod płatności i ich limitów. 

### Zmienne decyzyjne:

Model wykorzystuje zmienne binarne do reprezentacji wyboru poszczególnych wariantów płatności dla każdego zamówienia

$$
x_{i,j} =
\begin{cases}
1 & \text{jeśli wariant } j \text{ dla zamówienia } i \text{ zostanie wybrany} \\
0 & \text{w przeciwnym razie}
\end{cases}
$$

### Funkcja celu:

Maksymalizacja sumarycznego rabatu dla wszystkich zamówień:

$$
\max \sum_{i} \sum_{j} rabat_{i,j} \cdot x_{i,j}
$$

gdzie $rabat_{i,j}$ to rabat uzyskany z wariantu $j$ dla zamówienia $i$.

### Ograniczenia:

1. **Każde zamówienie musi mieć dokładnie jeden wariant:**

$$
\forall i \quad \sum_{j} x_{i,j} = 1
$$

2. **Każda metoda płatności ma swój limit:**

$$
\forall m \in \mathcal{M} \quad \sum_{i} \sum_{j} a_{i,j}^m \cdot x_{i,j} \leq \text{limit}_m
$$

gdzie $a_{i,j}^m$ to kwota przypisana do metody płatności $m$ w wariancie $j$ dla zamówienia $i$

---

## Dystrybucja punktów lojalnościowych

Po zakończeniu głównego procesu optymalizacji, w którym określane są preferowane warianty płatności, wybierane są te opcje, w których płatność jest częściowo dokonywana za pomocą punktów lojalnościowych oraz częściowo inną metodą tradycyjną.

W tych przypadkach, kwota przypisana do punktów lojalnościowych jest minimalna i wynosi dokładnie 10% wartości transakcji. Celem tej heurystyki jest **zwiększenie udziału punktów lojalnościowych** w takich wariantach płatności, jeśli pozostały jeszcze jakieś punkty i pod warunkiem, że nie zmienia to przyznanego rabatu.

### Zasada działania:

Dla każdego wariantu płatności, który korzysta zarówno z punktów lojalnościowych, jak i tradycyjnej metody płatności, system realizuje następujące kroki:

* **Krok 1**: Zidentyfikowanie metody tradycyjnej oraz kwoty przypisanej do tej metody w wariancie płatności.
* **Krok 2**: Sprawdzenie, czy pozostała liczba punktów lojalnościowych jest mniejsza niż kwota przypisana do metody tradycyjnej:

    * Przenosimy tylko tyle punktów, ile jest dostępne, zmniejszając odpowiednio kwotę przypisaną do tradycyjnej metody.
    * Używamy całych dostępnych punktów lojalnościowych, a proces kończy się.

## **Twierdzenie o poprawności przydziału punktów lojalnościowych:**

Jeśli po rozwiązaniu optymalizacyjnym zostały niewykorzystane punkty lojalnościowe (tj. $\texttt{remainingPoints} > 0$), to w rozwiązaniu istnieje przynajmniej jedno zamówienie, dla którego suma płatności metodami innymi niż "PUNKTY" jest większa lub równa tej pozostałej liczbie punktów albo większe zużycie punktów poskutkuje zmniejszeniem rabatu:

$$
\exists\ v \in \mathcal{S}:\quad \sum_{m \ne \text{"PUNKTY"}} v.\text{methods}[m] \ge \texttt{remainingPoints}
$$

gdzie:

* $\mathcal{S}$ – zbiór wariantów płatności wybranych przez solver (po optymalizacji),
* $v.\text{methods}[m]$ – kwota zapłacona metodą $m$,
* $\texttt{remainingPoints}$ – liczba niewykorzystanych punktów w limicie.

## **Dowód (intuicyjny, przez analizę zachowania solvera i rabatów):**

Zakładamy:

* Po rozwiązaniu pozostały punkty: $\texttt{remainingPoints} > 0$.

### **Przypadek 1: rabat za pełne użycie punktów >= 10%**

W tej sytuacji płacenie w całości punktami jest korzystniejsze niż dowolna kombinacja częściowej płatności punktami + kartą.

#### Co zrobiłby solver?

Jeśli dla jakiegoś zamówienia możliwe byłoby zapłacenie całości punktami, a rabat za to byłby większy niż 10%, to solver by to zrobił — jest to lepszy rabat, a mamy na to zasoby. Ale skoro tak się nie stało (czyli punkty nie zostały użyte w całości), to solver **nie miał wystarczającej liczby punktów** na pokrycie tego zamówienia w całości. Innymi słowy:

$$
v_i > \texttt{remainingPoints}
\Rightarrow
\sum_{m \ne \text{"PUNKTY"}} v.\text{methods}[m] = v_i - \texttt{usedPoints} > \texttt{remainingPoints}
$$

czyli część zapłacona kartą **musi być większa niż pozostałe punkty**.

### **Przypadek 2: rabat za pełne użycie punktów < 10%**

W tej sytuacji bardziej opłaca się użyć punktów częściowo (np. 10% zamówienia), by uzyskać lepszy rabat.

Solver więc wybierze wariant:

* 10% płatność punktami,
* reszta tradycyjnie.

Zatem pozostałe punkty możemy przydzielić do zamówień tylko wtedy, gdy nie poskutkuje to zapłatą za całe zamówienie punktami, czyli w konsekwencji zmniejszeniem rabatu.

### **Wniosek**

W każdej konfiguracji rabatów, jeśli po optymalizacji zostały punkty, to solver nie wykorzystał ich, bo nie mógł: albo **zabrakło punktów na pełną płatność**, albo zwiększenie ich udziału spowodowałoby **obniżenie rabatu**.

---

## Implementacja i Narzędzia

### 1. **Wersja Javy**

Projekt jest zaprojektowany do działania na **Javie 17**.

### 2. **Główne zależności**

* **Google OR-Tools (ortools-java)** to biblioteka, która stanowi fundament aplikacji, umożliwiając rozwiązywanie problemu optymalizacyjnego.

  ```xml
  <dependency>
    <groupId>com.google.ortools</groupId>
    <artifactId>ortools-java</artifactId>
    <version>9.12.4544</version>
  </dependency>
  ```

* **Jackson Databind (jackson-databind)** jest wykorzystywany do **serializacji i deserializacji** obiektów Java w formacie JSON.

  ```xml
  <dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.16.1</version>
  </dependency>
  ```
  
* **Lombok** to biblioteka, która automatycznie generuje kod, taki jak **gettery**, **settery**, **konstruktory**. Dzięki temu kod staje się bardziej zwięzły i łatwiejszy do zarządzania.

  ```xml
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.30</version>
    <scope>provided</scope>
  </dependency>
  ```

* **JUnit 5 (junit-jupiter)** to framework do testowania jednostkowego.

  ```xml
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
  </dependency>
  ```

### 3. **Budowanie i instalacja**

Projekt korzysta z **Mavena**, który automatycznie zarządza zależnościami, kompilacją i testowaniem projektu. Dzięki temu całość procesu budowania jest uproszczona.

* **maven-shade-plugin** to plugin, dzięki któremu projekt jest pakowany do tzw. **"fat JAR"**, który zawiera wszystkie wymagane zależności. 

* **formatter-maven-plugin** zapewnia automatyczne **formatowanie kodu** źródłowego. Pozwala to na zachowanie jednolitego stylu w całym projekcie.

### 4. **Korzystanie z projektu**

* **Budowanie Projektu**
  Aby zbudować projekt i utworzyć plik JAR, należy użyć komendy:

  ```bash
  mvn clean install
  ```

* **Uruchamianie Aplikacji**
  Po zbudowaniu aplikacji, można ją uruchomić za pomocą polecenia:

  ```bash
  java -jar target/optimus-price-1.0.0.jar <orders_file> <paymentmethods_file>
  ```

* **Testowanie**
  Aby uruchomić testy jednostkowe, należy wykonać poniższą komendę:

  ```bash
  mvn test
  ```

* **Formatowanie Kodu**
  Aby sformatować kod źródłowy zgodnie z przyjętymi wytycznymi, użyj komendy:

  ```bash
  mvn formatter:format
  ```

---
