package com.mapprjct.exceptions

import java.lang.IllegalArgumentException

class ElementAlreadyExistsException(message : String) : IllegalStateException(message)
class NotFoundException(message : String) : IllegalArgumentException(message)