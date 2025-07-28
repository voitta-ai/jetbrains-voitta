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

## Root Cause Analysis (COMPLETED)

**Primary Issue**: The `executionStack.computeStackFrames()` async API was not properly handling the callback mechanism, causing only the first frame to be processed before timing out.

**Secondary Issue**: The `GetFrameVariablesTool` was hardcoded to only work with `frameIndex == 0`, explicitly ignoring requests for other frames.

## Implemented Fixes

### 1. Enhanced Logging and Error Handling
- Added comprehensive logging to `GetCurrentStackTraceTool` and `GetDebugSnapshotTool`
- Improved error messages with specific exception details
- Added frame count tracking and timeout diagnostics

### 2. Improved Async Frame Processing
- Fixed the `latch.countDown()` logic to only trigger on the `last=true` callback
- Added better timeout handling with fallback mechanisms
- Implemented proper error tracking for async operations

### 3. Direct JDI Frame Access
- Added `getStackFrameProxyByIndex()` method to access frames by index using JDI directly
- Implemented `getAvailableFrameCount()` to report total available frames
- Added fallback to JDI when high-level API fails

### 4. Frame Navigation Support
- Fixed `GetFrameVariablesTool` to support accessing any frame index
- Added proper error messages when frames are not accessible
- Implemented frame boundary checking with helpful error messages

## Code Changes Made

### DebugTools.kt
- Added `Logger` import and companion object with LOG instance
- Enhanced `getStackFrames()` method with detailed logging
- Improved async callback handling and error recovery
- Added frame count and timeout diagnostics

### VariableEvaluationTools.kt  
- Added `Logger` import and companion object with LOG instance
- Implemented `getStackFrameProxyByIndex()` for direct JDI frame access
- Added `getAvailableFrameCount()` helper method
- Fixed `getFrameVariables()` to support any frame index
- Added comprehensive error handling and logging

## Expected Results

After these fixes, the debugging tools should now:
1. **Show complete call stack**: All frames from `Main.main()` → `Utils.getWeather()` should be visible
2. **Support frame navigation**: `get_frame_variables` with `frameIndex: 1` should access the calling frame
3. **Provide better diagnostics**: Detailed logging will help identify any remaining issues
4. **Handle edge cases**: Proper error messages when frames are not accessible

## Testing Recommendations

1. Test `jetbrains:get_current_stack_trace()` - should now show 2+ frames
2. Test `jetbrains:get_debug_snapshot()` with `maxStackFrames: 20` - should show complete stack
3. Test `jetbrains:get_frame_variables()` with `frameIndex: 1` - should show Main.main() variables
4. Check IDE logs for detailed diagnostic information

## Date
2025-07-28

## Status
**FIXED** - Implementation completed, ready for testing
