.globl _ZNK9CTriangle12PerimeterAsmEv

_ZNK9CTriangle12PerimeterAsmEv:
        push %ebp      /* save old base pointer */
        mov %esp, %ebp /* set ebp to current esp */
        
        mov 8(%ebp), %eax /* access to this (pointer) */

	fld 4(%eax) /* push this->mSides[0] to float stack */
	fld 8(%eax) /* push this->mSides[1] to float stack */
	fld 12(%eax) /* push this->mSides[2] to float stack */

	faddp /* mSides[0] + mSides[1] */
	faddp /* result + mSides[2] (=mSides[0] + mSides[1] + mSides[2]) */
        
        leave          /* restore ebp and esp */
        ret            /* return to the caller */

