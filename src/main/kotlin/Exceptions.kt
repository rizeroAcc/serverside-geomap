package com.mapprjct

import java.lang.IllegalArgumentException

class ElementAlreadyExistsException(message : String) : IllegalStateException(message)
class NotFoundException(message : String) : IllegalArgumentException(message)