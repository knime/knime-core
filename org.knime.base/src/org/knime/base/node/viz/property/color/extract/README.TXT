
Before merging this node onto the trunk, make sure to 
(a) delete this file
(b) open the API of ColorHandler to return the underlying ColorModel
    (the current implementation restores the color content by temporarily
    writing the model to a ModelContent object)