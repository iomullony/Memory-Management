import java.util.LinkedList;
import java.util.Random;

public class MemoryMgmt {
    
    private int memorySize = 8192;
    private LinkedList<MemoryChunk> memory = new LinkedList<>(); // main memory
    private LinkedList<MemoryChunk> freelist = new LinkedList<>(); //free list to store the free slots of memory
    private MemoryChunk head = new MemoryChunk(new Random().nextInt(200), 8, false); // head of the memory
    private MemoryChunk body = new MemoryChunk(head.getPointer() + head.getSize(), memorySize-8, true); // rest of the memory

    public MemoryMgmt() {
        memory.add(0, head);
        memory.add(1, body); // main memory
        freelist.add(body); // free list to store the free slots of memory

        System.out.println("\nHEAD Pointer: " + hexadecimal(head.getPointer()));
    }

    /**
     * Memory allocation, creates a chunk of memory and puts it in the main memory
     * 
     * @param size the size of the chunk of memory we want to allocate
     * @return the pointer where the chunk of memory starts
     */
    public int malloc(int size) {
        // allocates chunck of memory sized size
        MemoryChunk newChunk = new MemoryChunk(size, false);
        int index = -1; // default index
        int temp = memorySize; // temporary size where the chunk fits
        System.out.print("\nRequesting " + size + " bytes of memory... ");

        if (size >= 0) {
                        
            // I used best fit to allocate the memory (reduces fragmentation)
            // searches the best slot of memory in the  freelist
            for (int i = 0; i < freelist.size(); i++) {
                if (freelist.get(i).getSize() >= size && freelist.get(i).getSize() <= temp){ // checks the chunk fits here
                    temp = freelist.get(i).getSize();  // we save the size of this slot
                    index = i; // we save this index till we find some other slot
                }
            }
            
            if (index == -1) { // not enough memory
                if (sbrk(size) != -1) { // calls sbrk with the size of the memory we want to allocate
                    index = freelist.size()-1; // makes index the last index of the array list
                }
            }

            if (index > -1) {
                MemoryChunk Chosen = freelist.get(index); // gets the chosen slot of memory where we want to allocate out new chunk
                int ptr = Chosen.getPointer();
                int freeSize = Chosen.getSize() - size;
                int temp2 = 0;
                // reminder we didn't set the pointer yet
                newChunk.setPointer(ptr); // sets where the new chunk of memory is in the memory
                
                // adds to the main memory
                for (int i = 0; i < memory.size(); i++) {
                    if (memory.get(i).getPointer() == ptr) {
                        temp2 = i;
                    }
                }
                memory.set(temp2, newChunk);

                if (freeSize == 0) { // if the chunk occupies all the slot just remove it from the freelist
                    for (MemoryChunk f : freelist) {
                        if (f.getPointer() == ptr) {
                            freelist.remove(f);
                        }
                    }
                } else if (freeSize != 0){ // if the chunk of memory doesn't occupy the whole slot
                    int freePointer = ptr + size;
                    // creates a new chunk of memory to represent the remaining free maemory and adds it in the next index of the main memory
                    MemoryChunk freeChunk = new MemoryChunk(freePointer, freeSize, true);
                    memory.add(temp2+1, freeChunk);

                    // updates freelist
                    for (MemoryChunk f : freelist) {
                        if (f.getPointer() == ptr) {
                            f.setPointer(freePointer);
                            f.setSize(freeSize);
                        }
                    }

                    // points to the previous and next free slot
                    for (MemoryChunk m : memory) {
                        if (m.getPointer() < freePointer && m.isFree()) {
                            freeChunk.setPrev(m);
                        }
                        if (m.getPointer() > freePointer && m.isFree()) {
                            freeChunk.setNext(m);
                        }
                    }
                }
                System.out.println("memory allocated.");
                System.out.println("Pointer: " + hexadecimal(newChunk.getPointer()));

            } else System.out.println("\nERROR: Couldn't allocate memory.");
    
        } else System.out.println("\nERROR: Can't allocate negative memory.");
        
        return newChunk.getPointer();
    }

    /**
     * Frees the memory that the pointer determines
     * 
     * @param ptr the pointer of the chunk of memory we want to free
     */
    public void free(int ptr) {
        
        boolean freed = false; // checks if the memory has been freed or not
        System.out.print("\nFreeing pointer " + hexadecimal(ptr) + "... ");
        MemoryChunk freeChunk = null;

        for(int i  = 0; i < memory.size(); ++i){
            if (memory.get(i).getPointer() == ptr && memory.get(i).isOccupied()){

                memory.get(i).setFree(true); // sets to free the chunk in the main memory
                
                freeChunk = memory.get(i);
                
                int pointer = memory.get(i).getPointer();
                int index = 0;
                
                // updates freelist
                for (MemoryChunk f : freelist) {
                    if (f.getPointer() <= pointer && f.getNext() != null) {
                        if (f.getPointer() <= pointer && f.getNext().getPointer() > pointer) {
                            index = freelist.indexOf(f)+1;
                        }
                    } else if (f.getPointer() <= pointer && f.getNext() == null) {
                        index = freelist.indexOf(f) + 1;
                    }
                }
                freelist.add(index, freeChunk);

                freed = true;
                System.out.println("memory freed.");

            } else if (memory.get(i).getPointer() == ptr && memory.get(i).isFree()){ // if the slot was already free

                System.out.println("ERROR: This slot of memory is already free.");
                freed = true;
            }
        }

        // points to the previous and next free slot
        if (freeChunk != null) {
            for (MemoryChunk m : memory) {
                if (m.getPointer() < freeChunk.getPointer() && m.isFree()) {
                    freeChunk.setPrev(m);
                }
                if (m.getPointer() > freeChunk.getPointer() && m.isFree()) {
                    freeChunk.setNext(m);
                }
            }
        }
        if (!freed) System.out.println("ERROR: Invalid pointer.");
    }

    /**
     * Adds a new chunk of memory to the main memory of the required size to allocate the memory that was requested by malloc
     * 
     * @param size the size of the chunk of memory we want to allocate
     */
    public int sbrk(int size) {

        int exponent = 0;
        int temp = 0;
        int pointer = -1;

        if (size >= 0) {
            System.out.println("\nMemory limit exceeded, requesting further memory blocks... ");
            // the size should be the smallest power of two larger than the size requested
            while (temp < size) {
                temp = (int)Math.pow(2, exponent);
                exponent++;
            }
            
            int requestedSize = temp;
            // creates a new free chunk of memory with the requested size
            // the pointer for this chunk could be anything (while it's not inside the main memory)
            int min = memory.getLast().getPointer() + memory.getLast().getSize();
            MemoryChunk chunk = new MemoryChunk(new Random().nextInt(min, min + 300), requestedSize, true);
            pointer = chunk.getPointer();

            // adds chunk to the LinkedLists
            memory.addLast(chunk); 
            freelist.addLast(chunk);

            memorySize = memorySize + requestedSize; // makes the total memory size bigger
            System.out.print(requestedSize + " new bytes requested... ");
        }
        if (pointer == -1) System.out.println("ERROR: Couldn't request more memory.");

        return pointer;
    }

    /**
     * Runs a series of tests and prints the outputs
     */
    public void print() {

        // mandatory tests
        System.out.println("\nRunning test number 1...");
        MemoryMgmt test1 = new MemoryMgmt();
        int pointer1a = test1.malloc(28);
        test1.store(pointer1a, "Hello world");
        test1.retrieve(pointer1a);
        test1.free(pointer1a);
        test1.memoryState();
        
        System.out.println("\n\nRunning test number 2...");
        MemoryMgmt test2 = new MemoryMgmt();
        int pointer1b = test2.malloc(28);
        int pointer2b = test2.malloc(1024);
        int pointer3b = test2.malloc(28);
        test2.free(pointer2b);
        int pointer4b = test2.malloc(512);
        test2.free(pointer1b);
        test2.free(pointer3b);
        test2.memoryState();
        
        System.out.println("\n\nRunning test number 3...");
        MemoryMgmt test3 = new MemoryMgmt();
        int pointer1c = test3.malloc(7168);
        int pointer2c = test3.malloc(1024);
        test3.free(pointer1c);
        test3.free(pointer2c);
        test3.memoryState();

        System.out.println("\n\nRunning test number 4...");
        MemoryMgmt test4 = new MemoryMgmt();
        int pointer1d = test4.malloc(1024);
        int pointer2d = test4.malloc(28);
        test4.free(pointer2d);
        test4.free(pointer2d);
        test4.memoryState();
        
        // extra tests
        System.out.println("\n\nRunning test number 5...");
        MemoryMgmt test5 = new MemoryMgmt();
        int pointer1e = test5.malloc(7168);
        int pointer2e = test5.malloc(512);
        int pointer3e = test5.malloc(1500);
        test5.free(pointer2e);
        test5.retrieve(2);
        test5.memoryState();
        
        System.out.println("\n\nRunning test number 6...");
        MemoryMgmt test6 = new MemoryMgmt();
        int pointer1f = test6.malloc(1024);
        int pointer2f = test6.malloc(1024);
        test6.free(pointer1f);
        int pointer3f = test6.malloc(1024);
        int pointer4f = test6.malloc(1024);
        test6.free(pointer2f);
        test6.free(pointer3f);
        test6.free(pointer4f);
        test6.free(pointer1f);
        test6.memoryState();

        System.out.println("\n\nRunning test number 7...");
        MemoryMgmt test7 = new MemoryMgmt();
        int pointer1g = test7.malloc(5);
        test7.store(pointer1g, 3489);
        int pointer2g = test7.malloc(4);
        test7.retrieve(pointer1g);
        test7.store(pointer2g, "something");
        test7.retrieve(pointer2g);
        test7.memoryState();
        
        System.out.println("\n\nRunning test number 8...");
        MemoryMgmt test8 = new MemoryMgmt();
        int pointer1h = test8.malloc(1000);
        test8.free(pointer1h);
        int pointer2h = test8.malloc(-256);
        int pointer3h = test8.malloc(445435329);
        test8.store(pointer3h, 658778268);
        test8.store(pointer3h, "hello");
        test8.retrieve(pointer3h);
        test8.retrieve(pointer1h);
        test8.memoryState();
    }

    /**
     * Creates a table to show the state of all the memory
     */
    private void memoryState(){

        String leftAlignFormat = "| %-15s | %-15s | %-15s |%n";

        System.out.println("\nFinal memory state: ");
        System.out.format("+-----------------+-----------------+-----------------+%n");
        System.out.format("| Pointer         | Size            | Free            |%n");
        System.out.format("+-----------------+-----------------+-----------------+%n");

        for(int i  = 1; i < memory.size(); ++i){
            String pointer = hexadecimal(memory.get(i).getPointer());
            int size = memory.get(i).getSize();
            String status;

            // just to read better which slots are free and which ones occupied
            if (memory.get(i).isFree()) status = "Free";
            else status = "Occuppied";
            
            System.out.format(leftAlignFormat, pointer, size, status);
        }
        System.out.format("+-----------------+-----------------+-----------------+%n");
    }

    /**
     * Stores some information you put in a specific chunk of memory
     * 
     * @param ptr the pointer of the chunk of memory where you want to store the information
     * @param object the information you want to store
     */
    public void store(int ptr, Object object) {

        boolean stored = false; // to know if the information hias been stored or not
        System.out.print("\nStoring '" + object + "'... ");

        for (MemoryChunk m : memory) {
            if (m.getPointer() == ptr && m.getObject() == null) {
                // we want to store an integer
                if (object instanceof Integer) {
                    int integer = (int) object;

                    if (4 <= m.getSize()) { // 4 because an integer is 4 bytes
                        m.setObject(integer);
                        stored = true;
                    }
                } 
                // we want to store a string
                else if (object instanceof String) {
                    String string = (String) object;
                    int counter = 0;
                    
                    for (int i = 1; i <= string.length(); i++) { // each character is 1 byte, so we have to count how many characters are
                        counter++;
                    }
                    
                    if (counter <= m.getSize()) {
                        m.setObject(string);
                        stored = true;
                    }
                } 
                // we want to store a character
                else if (object instanceof Character) {
                    Character character = (Character) object;

                    if (1 <= m.getSize()) { // 1 because a character is 1 byte
                        m.setObject(character);
                        stored = true;
                    }
                } else System.out.print("\nERROR: Invalid data type.");
            } 
        }
        if (stored) System.out.println("stored sucessfully");
        else System.out.println("ERROR: Couldn't store the data.");
    }

    /**
     * Reads what the chunk of memory has stored if it has something
     * 
     * @param ptr the pointer of the chunck memory we want to retrieve
     */
    public void retrieve(int ptr) {

        System.out.print("\nRetrieving information... ");
        boolean retrieved = false;

        for (MemoryChunk m : memory) {
            if (m.getPointer() == ptr && m.getObject() != null) {
                System.out.println("Result: " + m.getObject());
                retrieved = true;
            }
        }
        if (!retrieved) System.out.println("ERROR: Couldn't retrieve information.");
    }

    /**
     * Converts the pointers to hexadecimal (just to look better)
     * 
     * @param ptr the pointer we want to convert
     * @return the string with the converted number
     */
    public String hexadecimal(int ptr) {
        String pointer = Integer.toHexString(ptr);
        for (int j = pointer.length(); j < 4; j++) {
            pointer = 0 + pointer;
        }
        pointer = "0x" + pointer;
        return pointer;
    }

    public static void main(String[] args) {
        new MemoryMgmt().print();
    }

    // A class to represent the chunks of memory
    private class MemoryChunk {
        private int ptr;
        private int size;
        private boolean is_free;
        private Object object = null;
        private MemoryChunk next = null;
        private MemoryChunk prev = null;
        
        public MemoryChunk(int size, boolean is_free) {
            this.size = size;
            this.is_free = is_free;
        }

        public MemoryChunk(int ptr, int size, boolean is_free) {
            this.ptr = ptr;
            this.size = size;
            this.is_free = is_free;
        }

        public int getPointer() {
            return ptr;
        }
        
        public int getSize() {
            return size;
        }

        public Object getObject() {
            return object;
        }

        public void setPointer(int ptr) {
            this.ptr = ptr;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public boolean isFree() {
            return is_free;
        }
        
        public boolean isOccupied() {
            return !is_free;
        }

        public void setFree(boolean is_free) {
            this.is_free = is_free;
        }
        
        public MemoryChunk getNext() {
            return next;
        }

        public MemoryChunk getPrev() {
            return prev;
        }

        public void setNext(MemoryChunk next) {
            this.next = next;
        }

        public void setPrev(MemoryChunk prev) {
            this.prev = prev;
        }
    }
}