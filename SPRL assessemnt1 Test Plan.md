# SPRL assessemnt1 Test Plan



### **Group Test Plan**

1. Before testing, please take a moment to confirm whether the Alpha branch code is correct.
2. If there are code updates, you can directly modify in the Alpha branch, or modify in your own branch and then merge back to Alpha.
3. Perform modification tests inside each person’s own json folder.
4. After each test run starts, perform one git commit to ensure that the json modification for this run can be tracked. Use a proper git commit message:

# test: <message>

1. Test results must include: run time testing with 100ms, 200ms, 500ms, 800ms
2. Save test results in folder groupT_test_results — distinguish results per person.
3. **Results aggregation rule:** Since each person’s runtime environment is different, take the average of the runtime parameter from the three persons — round upward — to get the standard parameter configuration. Finally, choose the most reasonable parameter between the longest and shortest measured values (we likely prefer around 100ms) as the final agent parameter.

Let the three measured parameters be:

t_1, t_2, t_3

**Step 1: Average and round upward**



```math
T_{std} = \left\lceil \frac{t_1 + t_2 + t_3}{3} \right\rceil
```

**Step 2: Select the reasonable parameter (within the range of min and max, biased toward 100ms)**

```math
T_{final} = \arg\min_{t \in [\min(t_i), \max(t_i)]} \left|t - 100\text{ms}\right|
```



### **Agent Testing Plan (Xiu)**

Conduct 1000 games.

Horizontal testing:

    1. Use the final parameter to test against existing agents: (basicMCTS, MCTS, OSLA, RHEA, RMHC)
    2. Start testing with 5-players — test (basicMCTS, MCTS, RHEA, RMHC) first; after finishing, replace the lowest-winrate agent with OSLA
    3. From the 5-players set eliminate the weakest agent → enter 4-players testing
    4. Continue looping until 2-players remain

Vertical testing:

    1. RHEA_T (with our heuristic) vs RHEA_T (Score / random / no heuristic) — run 200, 300, 500 games
    2. RHEA_T (with our heuristic) vs RHEA (original version) — run 200, 300, 500 games