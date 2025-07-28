# Stack Frame Display Issue

## Problem Description

We have identified a discrepancy between the actual call stack and what the JetBrains debugging tools are reporting during debugging sessions.

## Evidence

### What the Tools Report
- `jetbrains:get_current_stack_trace()` returns only 1 frame:
  ```json
  [{
    "methodName": "getWeather",
    "className": "Utils", 
    "fileName": "Utils.java",
    "lineNumber": 95,
    "isInUserCode": true,
    "frameIndex": 0
  }]
  ```

- `jetbrains:get_debug_snapshot()` with `maxStackFrames: 20` also shows only 1 frame
- `jetbrains:get_frame_variables()` with `frameIndex: 1` and `frameIndex: 2` return the same variables as `frameIndex: 0`

### What Actually Exists
Using `jetbrains:evaluate_expression()` with `Thread.currentThread().getStackTrace()`:

```
Frame 0: java.base/java.lang.Thread.getStackTrace(Thread.java:2450)
Frame 1: ai.voitta.jetbrains.example.Utils.getWeather(Utils.java:95) ← CURRENT
Frame 2: ai.voitta.jetbrains.example.Main.main(Main.java:12)
```

**Stack trace length**: 3 frames

## Expected vs Actual

### Expected Behavior
The debugging tools should show the complete call stack with at least 2 user frames:
1. `Utils.getWeather()` (current frame)
2. `Main.main()` (calling frame)

### Actual Behavior  
The debugging tools only report the current frame, missing the calling context entirely.

## Impact

- **Limited debugging capability**: Cannot inspect variables in calling methods
- **No navigation between frames**: Cannot step up/down the call stack
- **Incomplete context**: Missing information about how we got to the current method
- **Tool reliability concerns**: Discrepancy between tool reports and runtime reality

## Test Case

- **Project**: `/Users/gregory.golberg/g/git.voitta/jetbrains-voitta/src/main/resources/SampleJavaProject`
- **Breakpoint**: `Utils.java:95` (return statement in `getWeather()`)
- **Call path**: `Main.main()` → `Utils.getWeather()`
- **Debug session**: Active with breakpoint hit

## Workaround

Use `jetbrains:evaluate_expression()` with `Thread.currentThread().getStackTrace()` to get the actual call stack information.

## Next Steps

1. Investigate why the frame enumeration tools are not detecting multiple frames
2. Test with different breakpoint locations and call depths
3. Verify if this is a tool configuration issue or a deeper problem with the debugging integration
4. Consider alternative approaches for accessing frame information

## Date
2025-07-28

## Status
Open - Needs investigation
