.globl _ZNK7CCircle7AreaAsmEv

_ZNK7CCircle7AreaAsmEv:
        push %ebp      /* save old base pointer */
        mov %esp, %ebp /* set ebp to current esp */
        #acces the pointer this
        mov 8(%ebp), %eax 

        fld 4(%eax) /* push this->radius to float stack */
	fld 4(%eax) /* push this->radius to float stack */
	#push pi
        fldpi 
        #pi * radius
	fmulp 
	#pi*radius*radius
        fmulp 
        
        leave          /* restore ebp and esp */
        ret            /* return to the caller */
