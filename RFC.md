## Implementing `break` and `continue`
grammar: 
- breakBlock => break || (statement)* break (statement)* 
- break => "break"

strategy:
1. Shared hash
- A hash shared with the for loop to update an early return flag `outerLoopHash: String`
- add hashmap to interpreter where value is running flag or update visitors to return early exit flag?