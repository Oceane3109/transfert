(10,9);
           farany=valiny*lavitrisa;  
        }

    return farany;
}


int valinyfarany(char**mots)
{

}


int main()
{
    char*tab;
    tab=malloc(13*sizeof(char));
    tab[0]='r';
    tab[1]='o';
    tab[2]='a';
    tab[3]=' ';
    tab[4]='l';
    tab[5]='a';
    tab[6]='v';
    tab[7]='i';
    tab[8]='t';
    tab[9]='r';
    tab[10]='i';
    tab[11]='s';
    tab[12]='a';

    int valiny=suite(tab);
    printf("%i",valiny);

}

