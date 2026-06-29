Run the multi-agent banking pipeline end-to-end (Kotlin).

Steps:
1. Check that `sample-transactions.json` exists in `homework-6-kotlin/`
2. Clear `shared/input/`, `shared/processing/`, and `shared/output/` (results are preserved unless explicitly cleared)
3. Run the pipeline: `cd homework-6-kotlin && ./gradlew run`
4. Show a summary of results from `shared/results/pipeline_summary.json`
5. Report any transactions that were rejected and why (from `shared/results/TXN*.json`)