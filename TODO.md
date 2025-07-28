1. We correctly identify first line of the method to set the breakpoint in but somehow not the last one (for the last one, we set it at the closing } at the end of the block)

2. We seem to only be able to look at Frame0 when stopped at breakpoint.

3. Debug snapshot is not showing local variables.
