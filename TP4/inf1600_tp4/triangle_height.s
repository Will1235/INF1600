.data
        factor: .float 2.0 /* use this to multiply by two */

.text
.globl	_ZNK9CTriangle9HeightAsmEv

_ZNK9CTriangle9HeightAsmEv:
        push %ebp      /* save old base pointer */
        mov %esp, %ebp /* set ebp to current esp */
        
        mov 8(%ebp), %eax /* access to this (pointer) */
	mov 0(%eax), %ecx /* ecx contains virtual table pointer */

	push %eax /* stacking "this" pointer */
	call *20(%ecx) /* calling CTriangle::AreaAsm from vtable */
	add $4, %esp /* clean parameters */

	/* st[0] contains the area */

	fld 12(%eax) /* load mSides[2] to float stack */
	fdivrp	/* st[0] = A / mSides[2] */

	fld factor /* push 2.0f to float stack */
	fmulp /* st[0] = 2.0f * A / mSides[2] */
        
        leave          /* restore ebp and esp */
        ret            /* return to the caller */

