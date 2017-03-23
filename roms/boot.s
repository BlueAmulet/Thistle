.memorymap
defaultslot  0
slotsize     $1000
slot 0       $F000 ; eeprom
.endme

.rombankmap
bankstotal  1
banksize    $1000 ; EEPROM
banks       1
.endro

.emptyfill $FF

.bank 0 slot 0
.org $0000

.db "--[[CABE:Thistle:"

; TSF Component Data
fslist: .db 10,10,0,"filesystem",0 ; 14
umlist: .db 10,5,0,"drive",0 ; 9
secread: .db 10,10,0,"readSector",3,1,0 ; 16
fsopend: .db 10,4,0,"open",10,12,0,"Thistle/boot",0 ; 23
fsopenf: .db 10,4,0,"open",10,7,0,"Thistle",0 ; 18
fsclose: .db 10,5,0,"close" ; 8
fsread: .db 10,4,0,"read",14,0,0,0,0,4,0,1,0 ; 16

; Messages
greeting: .db "Thistle Boot ROM",10,10
nomem: .db "No Memory installed"
drives: .db "Checking drives ...",10 ; 20
fsmsg: .db 10,"Checking filesystems ...",10 ; 26
bootmsg .db "Booting ...",10 ; 12
noboot: .db "Nothing to boot from"

hexlookup: .db "0123456789abcdef"

.macro _copy_base_short
	lda #<\1
	sta $E041
	lda #>\1
	sta $E042
	lda #<\2
	sta $E043
	lda #>\2
	sta $E044
	lda #<\3
	sta $E045
.endm

.macro copys_pp
	_copy_base_short \1 \2 \3
	lda #$00
	sta $e040
.endm

.macro copys_pu
	_copy_base_short \1 \2 \3
	lda #$01
	sta $e040
.endm

.macro copys_up
	_copy_base_short \1 \2 \3
	lda #$02
	sta $e040
.endm

.macro copys_uu
	_copy_base_short \1 \2 \3
	lda #$03
	sta $e040
.endm

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
--	lda ($00),Y
	jsr hexprint
	iny
	cpy #$10
	beq ++
	cpy #$04
	beq +
	cpy #$06
	beq +
	cpy #$08
	beq +
	cpy #$0A
	beq +
	jmp --
+	lda #$2D
	sta $E003
	jmp --
++	lda #$0a
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
--	lda $E012
	cmp #$00 ; TSF End Tag
	beq ++

	; Read UUID
	ldx #$10
-	lda $E012
	sta ($00),Y
	iny
	cpy #$00
	bne +
	inc $01 ; Increment $01 when Y wraps to 0
+	dex
	cpx #$00
	bne -

	; Drop component name
	ldx $02
-	lda $E012
	dex
	cpx #$00
	bne -
	jmp --
++	lda #$02
	sta $01
	rts

.macro readlist
	lda #\1
	sta $02
	jsr _readlist
.endm

loaduuid:
	; Load a UUID into the component selector buffer
	; $00, $01 - Address to read from
	; Clobbers: A, X, Y, (uuidprint)
	jsr uuidprint
	ldy $00
	ldx #$10
	lda #$0b ; UUID Tag
	sta $E012
-	lda ($00),Y ; UUID Byte loop
	sta $E012
	iny
	cpy #$00
	bne +
	inc $01
+ dex
	cpx #$00
	bne -
	sty $00
	stx $E012 ; End Tag
	stx $E010 ; Map Component
	rts

bootdrive:
	; Checks and boots from a drive
	lda $D000
	cmp #$00
	beq +
	rts
+	copys_up bootmsg $E003 12
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
	beq +
	rts
+	copys_up bootmsg $E003 12
	lda #$02 ; Something to boot!
	sta $00
	copys_uu fsread $0001 16 ; Copy read command
	copys_pu $D001 $0008 5 ; Inject handle

-	ldx $00
	cpx #$D0
	beq ++ ; Too much data read
	copys_up $0001 $D001 16 ; Call "read"
	lda #$00
	sta $D000

	lda $D001 ; Check TSF Tag
	cmp #$09 ; Byte array?
	beq +
	cmp #$0A ; String?
	beq +
	jmp ++ ; No more data to read

+	ldy #$01 ; Setup Copy Engine
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
	jmp -

++	copys_up fsclose $D001 8 ; Call close
	copys_up $0008 $D001 5
	stx $D001
	stx $D000
	copys_up $0008 $E012 5 ; Destroy value
	stx $E012
	lda #$04
	sta $E010
	jmp $0200 ; Boot

reset:
	; Display boot greeting
	copys_up greeting $E003 18

	; Memory Check
	lda $E018
	cmp #$00
	bne havemem
	lda $E019
	cmp #$00
	bne havemem
	; No Memory Installed
	copys_up nomem $E003 19
-	jmp -

havemem:
	copys_up drives $E003 20
	; Look for "drive" components
	copys_up umlist $E012 9
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
	copys_up secread $D001 16 ; Call readSector
	lda #$00
	sta $D000
	jsr bootdrive
	dec $03

	; Check the drive components

fschk:
	copys_up fsmsg $E003 26
	; Look for "filesystem" components
	copys_up fslist $E012 14
	lda #$03
	sta $E010

	; Store list to memory
	lda $E011
	sta $03
	readlist 13

	; Parse list
-	lda $03
	cmp #$00
	beq failboot ; No "filesystem" componets left to check
	jsr loaduuid
	copys_up fsopend $D001 23 ; open Thistle/boot
	lda #$00
	sta $D000
	jsr bootfs
	copys_up fsopenf $D001 18 ; open Thistle
	lda #$00
	sta $D000
	jsr bootfs
	dec $03
	jmp -

failboot:
	copys_up noboot $E003 20
-	jmp -

nmi:
	rti

irq:
	rti

.db "]]error\"Thistle architecture required\"--"

.orga $FFFA
.dw nmi
.dw reset
.dw irq
