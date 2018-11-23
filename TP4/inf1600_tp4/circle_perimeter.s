.data
        factor: .float 2.0 /* use this to multiply by two */

.text
.globl _ZNK7CCircle12PerimeterAsmEv

_ZNK7CCircle12PerimeterAsmEv:
        push %ebp      /* save old base pointer */
        mov %esp, %ebp /* set ebp to current esp */
        #access this
        mov 8(%ebp), %eax 

        fld 4(%eax) /* push this->radius to float stack */
	#push pi
        fldpi 
        #push 2.0f
	fld factor 
        #2.0f * pi
	fmulp 
        #2.0f * pi * radius
	fmulp 
        
        leave          /* restore ebp and esp */
        ret            /* return to the caller */
