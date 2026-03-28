# KNN uruchamianie i eksperymenty

## Pojedynczy run

Parametry:
- `k`
- `features` (lista po przecinku)
- `testPercent`
- `metric` (`MANHATTAN` lub `EUCLIDEAN`)
- `seed`

Przykład:

```powershell
.\gradlew.bat :app:run --args="--k=5 --testPercent=20 --metric=MANHATTAN --features=alnumToOtherCharsRatio,averageWordLength,wordCount"
```

## Runner eksperymentów

Klasa: `org.bir.knn.KnnAutomatedTestRunner`

Możesz uruchomić przez Gradle task `:app:runKnnAutomation` (argumenty podawane przez `-PappArgs`).

```powershell
.\gradlew.bat :app:runKnnAutomation -PappArgs="--scenario=all --threads=4 --metrics=MANHATTAN,EUCLIDEAN --k=5 --testPercent=20 --maxFeatureDrop=4 --out=knn-results.csv"
```

Runner drukuje progress na bieżąco (`[kSweep]`, `[splitSweep]`, `[featureSweep]`) wraz z accuracy.

Scenariusze:
- `kSweep`: szuka najlepszej kombinacji `k + metric` (tie-break: mniejsze `k`, mniej cech, potem metryka), `k` od 1 do 3 kolejnych pogorszeń accuracy
- `splitSweep`: dla podanego `k`, metryki i cech szuka najlepszego podziału train/test (train od 10% do 90% co 5%)
- `featureSweep`: dla podanego `k`, metryki i `testPercent` szuka najlepszego zestawu cech (usunięcie max `maxFeatureDrop`)

Przykłady uruchomień pojedynczych scenariuszy:

```powershell
.\gradlew.bat :app:runKnnAutomation -PappArgs="--scenario=ksweep --threads=2 --metrics=MANHATTAN,EUCLIDEAN --testPercent=20 --maxK=200 --out=ksweep.csv"
.\gradlew.bat :app:runKnnAutomation -PappArgs="--scenario=splitsweep --threads=2 --k=5 --metrics=MANHATTAN,EUCLIDEAN --features=alnumToOtherCharsRatio,averageWordLength,charCount,lexicalDiversity,longWordToOtherWordsRatio,properNounMidSentenceRatio,properNounFirst,rarestRepeatedWord,upperToAllCharsRatio,upperToLowerRatio,wordCount --out=splitsweep.csv"
.\gradlew.bat :app:runKnnAutomation -PappArgs="--scenario=featuresweep --threads=2 --k=5 --metrics=MANHATTAN,EUCLIDEAN --testPercent=20 --maxFeatureDrop=4 --features=alnumToOtherCharsRatio,averageWordLength,charCount,lexicalDiversity,longWordToOtherWordsRatio,properNounMidSentenceRatio,properNounFirst,rarestRepeatedWord,upperToAllCharsRatio,upperToLowerRatio,wordCount --out=featuresweep.csv"
```

CSV zawiera kolumny:
- `k`
- `test_size_pct`
- `metric`
- `features`
- `accuracy`


