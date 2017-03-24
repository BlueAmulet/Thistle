;
; Thistle Boot ROM
;
	.setcpu		"6502"

.segment	"STARTUP"

.byte "--[[CABE:Thistle:"

; TSF Component Data
fslist: .byte 10,10,0,"filesystem",0
umlist: .byte 10,5,0,"drive",0
secread: .byte 10,10,0,"readSector",3,1,0
fsopend: .byte 10,4,0,"open",10,12,0,"Thistle/boot",0
fsopenf: .byte 10,4,0,"open",10,7,0,"Thistle",0
fsclose: .byte 10,5,0,"close"
fsread: .byte 10,4,0,"read",14,0,0,0,0,4,0,1,0

; Messages
greeting: .byte "Thistle Boot ROM",10,10
nomem: .byte "No Memory installed"
drives: .byte "Checking drives ...",10
fsmsg: .byte 10,"Checking filesystems ...",10
bootmsg: .byte "Booting ...",10
noboot: .byte "Nothing to boot from"

hexlookup: .byte "0123456789abcdef"

.macro _copy_base_short src, dest, len, mode
	lda #<src
	sta $E041
	lda #>src
	sta $E042
	lda #<dest
	sta $E043
	lda #>dest
	sta $E044
	lda #<len
	sta $E045
	lda #mode
	sta $e040
.endmacro

.macro copys_pp src, dest, len
	_copy_base_short src, dest, len, $00
.endmacro

.macro copys_pu src, dest, len
	_copy_base_short src, dest, len, $01
.endmacro

.macro copys_up src, dest, len
	_copy_base_short src, dest, len, $02
.endmacro

.macro copys_uu src, dest, len
	_copy_base_short src, dest, len, $03
.endmacro

hexprint:
	; Prints a byte to the screen
	; A - byte to print
	; Clobbers: X
	pha
	tax
	tya
	pha
	txa
	lsr
	lsr
	lsr
	lsr
	tay
	lda hexlookup,Y
	sta $E003
	txa
	and #$0F
	tay
	lda hexlookup,Y
	sta $E003
	pla
	tay
	pla
	rts

uuidprint:
	; Prints a UUID to the screen
	; $00, $01 - Address of UUID
	; Clobbers: A, X (hexprint), Y
	ldy #$00
@loop:	lda ($00),Y
	jsr hexprint
	iny
	cpy #$10
	beq @done
	cpy #$04
	beq @dash
	cpy #$06
	beq @dash
	cpy #$08
	beq @dash
	cpy #$0A
	beq @dash
	jmp @loop
@dash:	lda #$2D
	sta $E003
	jmp @loop
@done:	lda #$0a
	sta $E003
	rts

_readlist:
	; Reads component list to $0200
	; $02 - Bytes to skip for component type
	; Clobbers: A, X, Y, $00, $01
	lda #$00
	sta $00
	tay
	lda #$02
	sta $01
@loop1:	lda $E012
	cmp #$00 ; TSF End Tag
	beq @done

	; Read UUID
	ldx #$10
@loop2:	lda $E012
	sta ($00),Y
	iny
	cpy #$00
	bne @skip
	inc $01 ; Increment $01 when Y wraps to 0
@skip:	dex
	cpx #$00
	bne @loop2

	; Drop component name
	ldx $02
@loop3:	lda $E012
	dex
	cpx #$00
	bne @loop3
	jmp @loop1
@done:	lda #$02
	sta $01
	rts

.macro readlist skip
	lda #skip
	sta $02
	jsr _readlist
.endmacro

loaduuid:
	; Load a UUID into the component selector buffer
	; $00, $01 - Address to read from
	; Clobbers: A, X, Y, (uuidprint)
	jsr uuidprint
	ldy $00
	lda #$00
	sta $00
	ldx #$10
	lda #$0b ; UUID Tag
	sta $E012
@loop:	lda ($00),Y ; UUID Byte loop
	sta $E012
	iny
	cpy #$00
	bne @skip
	inc $01
@skip:	dex
	cpx #$00
	bne @loop
	sty $00
	stx $E012 ; End Tag
	stx $E010 ; Map Component
	rts

bootdrive:
	; Checks and boots from a drive
	lda $D000
	cmp #$00
	beq @boot
	rts
@boot:	copys_up bootmsg, $E003, .sizeof(bootmsg)
	lda #$01 ; Setup Copy Engine
	sta $E041
	lda #$D0
	sta $E042
	lda #$00
	sta $E043
	lda #$02
	sta $E044

	lda $D001 ; Discard tag
	lda $D001
	sta $E045
	lda $D001
	sta $E046

	lda #$01 ; Copy
	sta $E040

	lda #$00
	sta $E046
	jmp $0200 ; Boot

bootfs:
	; Boots from a file
	lda $D000
	cmp #$00
	beq @boot
	rts
@boot:	copys_up bootmsg, $E003, .sizeof(bootmsg)
	lda #$02 ; Something to boot!
	sta $00
	copys_uu fsread, $0001, .sizeof(fsread) ; Copy read command
	copys_pu $D001, $0008, 5 ; Inject handle

@loop:	ldx $00
	cpx #$D0
	beq @done ; Too much data read
	copys_up $0001, $D001, .sizeof(fsread) ; Call "read"
	lda #$00
	sta $D000

	lda $D001 ; Check TSF Tag
	cmp #$09 ; Byte array?
	beq @skip
	cmp #$0A ; String?
	beq @skip
	jmp @done ; No more data to read

@skip:	ldy #$01 ; Setup Copy Engine
	sty $E041
	lda #$D0
	sta $E042
	ldx #$00
	stx $E043
	lda $00
	sta $E044
	lda $D001
	sta $E045
	lda $D001
	sta $E046
	sty $E040 ; Execute Copy Engine Command
	stx $E046 ; Put high byte of size back to 0
	inc $00
	jmp @loop

@done:	copys_up fsclose, $D001, .sizeof(fsclose) ; Call close
	copys_up $0008, $D001, 5
	stx $D001
	stx $D000
	copys_up $0008, $E012, 5 ; Destroy value
	stx $E012
	lda #$04
	sta $E010
	jmp $0200 ; Boot

reset:
	; Display boot greeting
	copys_up greeting, $E003, .sizeof(greeting)

	; Memory Check
	lda $E018
	cmp #$00
	bne havemem
	lda $E019
	cmp #$00
	bne havemem
	; No Memory Installed
	copys_up nomem, $E003, .sizeof(nomem)
@loop:	jmp @loop

havemem:
	copys_up drives, $E003, .sizeof(drives)
	; Look for "drive" components
	copys_up umlist, $E012, .sizeof(umlist)
	lda #$03
	sta $E010

	; Store list to memory
	lda $E011
	sta $03
	readlist 8

	; Parse list
	lda $03
	cmp #$00
	beq fschk ; No "drive" componets left to check
	jsr loaduuid
	copys_up secread, $D001, .sizeof(secread) ; Call readSector
	lda #$00
	sta $D000
	jsr bootdrive
	dec $03

	; Check the drive components

fschk:
	copys_up fsmsg, $E003, .sizeof(fsmsg)
	; Look for "filesystem" components
	copys_up fslist, $E012, .sizeof(fslist)
	lda #$03
	sta $E010

	; Store list to memory
	lda $E011
	sta $03
	readlist 13

	; Parse list
@loop:	lda $03
	cmp #$00
	beq failboot ; No "filesystem" componets left to check
	jsr loaduuid
	copys_up fsopend, $D001, .sizeof(fsopend) ; open Thistle/boot
	lda #$00
	sta $D000
	jsr bootfs
	copys_up fsopenf, $D001, .sizeof(fsopenf) ; open Thistle
	lda #$00
	sta $D000
	jsr bootfs
	dec $03
	jmp @loop

failboot:
	copys_up noboot, $E003, .sizeof(noboot)
@loop:	jmp @loop

nmi:
	rti

irq:
	rti

.byte "]]error",$22,"Thistle architecture required",$22,"--"

.segment	"VECTORS"

.word nmi
.word reset
.word irq
