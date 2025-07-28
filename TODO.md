We continue iterating on this project. A few touches remain. 

1. We correctly identify first line of the method to set the breakpoint in but somehow not the last one (for the last one, we set it at the closing } at the end of the block)

2. In terms of stack traces, We seem to only be able to look at Frame0 when stopped at breakpoint. Looks like the snapshot does not have the information about the entire stack? Can we enhance it?

3. Debug snapshot is not showing local variables. We are able to use evaluate expression functionality but it would be nice to have them in the snapshot itself.
