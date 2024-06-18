# RAF (School of Computing) - Concurrent and Distributed Systems Projects

This repository contains three projects developed during my Concurrent and Distributed Systems course. These projects progressively explore key concepts in this domain, ranging from concurrent matrix multiplication to fault-tolerant distributed systems with virtual file systems and friendship management.

## Projects

| Project Name           | Description                                                                                                         | Link                                                                                                                              |
| -----------------------| -------------------------------------------------------------------------------------------------------------       | ----------------------------------------------------------------------------------------------------------------------------------|
| **Matrix Solver**      | A concurrent system for matrix multiplication.                                                                      | [Link to Project 1](https://github.com/oprica9/concurrent-and-distributed-systems/tree/main/kids_d1_ognjen_prica_rn10620)         |
| **Distributed Global Snapshots**          | A distributed system supporting global snapshots of the system's state.                                       | [Link to Project 2](https://github.com/oprica9/concurrent-and-distributed-systems/tree/main/kids_d2_ognjen_prica_rn10620)         |
| **Rafbook**            | A demonstration of a fault-tolerant distributed system with a virtual file system and friendship management.        | [Link to Project 3](https://github.com/oprica9/concurrent-and-distributed-systems/tree/main/kids_pr_ognjen_prica_rn10620)         |

## Project Details

### Project 1: Matrix Solver

**Description:**

The Matrix Solver is a concurrent system designed to multiply matrices. It reads matrices from text files (with .rix extension), stores the results, and provides a command-line interface for users to interact with the system.  

**Key Features:**

*   **Concurrency:** Utilizes thread pools and concurrent data structures to handle multiple tasks simultaneously, improving performance on multi-core systems.
*   **Dynamic File Discovery:** Continuously scans directories for new or modified matrix files, ensuring the system is always up-to-date.
*   **Optimized Workload Distribution:** Divides tasks into smaller chunks for parallel processing, maximizing resource utilization.
*   **Fault Tolerance:** Gracefully handles errors like invalid input or missing files, ensuring the system's robustness.
*   **Result Caching:** Caches calculation results to expedite repeated queries.

**Technologies Used:**

*   Java
*   Thread pools
*   Concurrent data structures

### Project 2: Distributed Global Snapshots

**Description:**

This project focuses on building a distributed system that supports snapshots of the system's state. It involves multiple nodes communicating asynchronously and the ability to initiate snapshots from specific initiator nodes.

**Key Concepts:**

*   **Distributed Systems:**  Implements a system with multiple nodes communicating and coordinating asynchronously.
*   **Non-FIFO Communication:**  Handles communication where messages may not arrive in the order they were sent.
*   **Snapshot Algorithms:** Implements a variation of the Lai-Yang algorithm, specifically Li et al.'s algorithm for capturing the global state of the system, as well as Spezialetti-Kearns algorithm for efficient dissemination of the recorded snapshot.
*   **Fault Tolerance:** Addresses specific error scenarios to maintain system functionality.

**Challenges:**

*   Implementation of the state reset functionality after a snapshot was *not* fully successful. The issue was that the non-fifo nature of the channels requires us to somehow handle the out-of-order messages. That was achieved by using a message buffer. Now,
the problem was that the buffer is a potential bottleneck for the system, because the messages it contains have to be processed before another snapshot starts, and it is not for certain that the snapshots won't be occuring at a relatively small time interval. The issue
can possibly be resolved by simply processing the messages in the buffer after the snapshot has been taken, but in the case of quickly occuring snapshots, by the time the process empties the buffer it may miss some incoming messages due to socket timeout (which of course
can be configured to be longer, but that would only be *hacky* and not a robust solution)

**Technologies Used:**

*   Java
*   Li et al. algorithm
*   Spezialetti-Kearns algorithm

### Project 3: Rafbook - A Fault-Tolerant and Topologically Optimized Distributed System Demo

**Description:**

Rafbook is a demonstration of a distributed system featuring an adaptive, fault-tolerant virtual file system and a friendship management system between processes.

**Key Features:**

*   **Chord DHT:** Uses the Chord Distributed Hash Table architecture for efficient data management and routing.
*   **Suzuki-Kasami Algorithm:**  Enables concurrent joining of multiple nodes and achieves mutual exclusion in critical sections.
*   **Hybrid Failure Detection:** Combines a buddy system and heartbeat system for robust failure detection and system recovery.
*   **File Replication:** Replicates public files across multiple nodes for fault tolerance.

**Achievements:**

*   Successful demonstration of a distributed system with fault tolerance and topological optimization.
*   Implementation of Chord DHT, Suzuki-Kasami algorithm, and hybrid failure detection.
*   Creation of a virtual file system with public and private file handling and replication.

**Technologies Used:**

*   Java
*   Chord DHT
*   Suzuki-Kasami algorithm
*   Buddy and Heartbeat mechanism

## Additional Notes:
* The second project and the third project were done using a class provided framework.
* Feel free to explore the project folders for code and further details.

