def outerFunction():
    text = "Hello"
 
    def innerFunction():
        nonlocal text
        text = f"{text}!"
        print(text)
 
    # Note we are returning function
    # WITHOUT parenthesis
    return innerFunction 
 
if __name__ == '__main__':
    myFunction = outerFunction()
    myFunction()
    myFunction()    
    myFunction()