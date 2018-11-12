from enum import Enum


class StatusCodes(Enum):
    RUNNING = "RUNNING"
    SCHEDULED = "SCHEDULED"
    DONE = "DONE"
    FAILED = "FAILED"
    UNKNOWN = "UNKNOWN"
    DROPPED = "DROPPED"