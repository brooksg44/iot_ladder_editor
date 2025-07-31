LD %I0.0    ; Load input bit 0 (start button)
ANDN %I0.1  ; AND with inverted input bit 1 (stop button)
OR %Q0.0    ; OR with previous output (seal-in)
ST %Q0.0    ; Store result to output bit 0 (motor)

LD %I0.2    ; Load input bit 2 (emergency stop)
STN %Q0.1   ; Store inverted to alarm output