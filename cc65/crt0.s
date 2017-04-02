;
; Startup code for cc65 (Thistle version)
;

        .export         _exit
        .export         __STARTUP__ : absolute = 1      ; Mark as startup

        .import         _main

        .include        "zeropage.inc"

.segment "STARTUP"

        jsr     _main
_exit:  jmp     _exit
